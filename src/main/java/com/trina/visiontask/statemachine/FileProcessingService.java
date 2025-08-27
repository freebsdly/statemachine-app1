package com.trina.visiontask.statemachine;

import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.mq.MessageProducer;
import com.trina.visiontask.service.TaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class FileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    private final StateMachineManager stateMachineManager;
    private final MessageProducer messageProducer;
    private final TaskConfiguration taskConfiguration;

    public FileProcessingService(
            StateMachineManager stateMachineManager,
            MessageProducer messageProducer,
            TaskConfiguration taskConfiguration
    ) {
        this.stateMachineManager = stateMachineManager;
        this.messageProducer = messageProducer;
        this.taskConfiguration = taskConfiguration;
    }

    public void processFile(FileProcessingState initState, FileProcessingEvent event, TaskDTO taskInfo)
            throws Exception {
        // 这里使用builder创建状态机，以便从指定的初始状态开始，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = stateMachineManager.acquireStateMachine(initState, taskInfo.getTaskId().toString());
        CountDownLatch completionLatch = new CountDownLatch(1);
        if (taskInfo.getStartTime() == null) {
            taskInfo.setStartTime(LocalDateTime.now());
        }
        StateMachineListenerAdapter<FileProcessingState, FileProcessingEvent> listener = new StateMachineListenerAdapter<>() {
            private StateContext<FileProcessingState, FileProcessingEvent> context;

            @Override
            public void stateContext(StateContext<FileProcessingState, FileProcessingEvent> stateContext) {
                this.context = stateContext;
            }

            @Override
            public void stateChanged(State<FileProcessingState, FileProcessingEvent> from, State<FileProcessingState, FileProcessingEvent> to) {
                if (from == null) {
                    // 初始化状态
                    return;
                }
                FileProcessingState source = from.getId();
                FileProcessingState target = to.getId();
                FileProcessingEvent event = context.getEvent();
                TaskDTO taskInfo = (TaskDTO) context.getMessage().getHeaders().get(taskConfiguration.getTaskInfoKey());
                log.info("State changed from {} to {}, triggered by {}", source, target, event);
                if (taskInfo != null) {
                    taskInfo.setPreviousState(source);
                    taskInfo.setCurrentState(target);
                    taskInfo.setEvent(event);
                    taskInfo.setEndTime(LocalDateTime.now());
                }
                messageProducer.sendToTaskLogQueue(taskInfo);
                switch (target) {
                    case UPLOADED -> messageProducer.sendToPdfConvertQueue(taskInfo);
                    case PDF_CONVERTED -> messageProducer.sendToMdConvertQueue(taskInfo);
                    case MARKDOWN_CONVERTED -> messageProducer.sendToAiSliceQueue(taskInfo);
                }
                // 检查是否到达可退出状态
                if (target == FileProcessingState.UPLOADED
                        || target == FileProcessingState.PDF_CONVERTED
                        || target == FileProcessingState.MARKDOWN_CONVERTED
                        || target == FileProcessingState.COMPLETED
                        || target == FileProcessingState.FAILED
                ) {
                    completionLatch.countDown();
                } else if (target == FileProcessingState.MARKDOWN_CONVERT_SUBMITTED
                        || target == FileProcessingState.AI_SLICE_SUBMITTED
                ) {
                    log.info("waiting for markdown or slice callback");
                }
            }
        };
        stateMachine.addStateListener(listener);
        stateMachine.startReactively().subscribe();
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                .build();
        // 发送初始事件，开始文件处理流程
        stateMachine.sendEvent(Mono.just(message)).subscribe();

        // 等待处理完成
        boolean finished = completionLatch.await(taskConfiguration.getWaitTimeout(), TimeUnit.SECONDS);
        if (finished) {
            log.info("File processing finished");
        } else {
            //TODO: 如果等待超时，需要统一处理，看是否需要重试
            log.warn("File processing timed out");
        }
        stateMachineManager.releaseStateMachine(taskInfo.getTaskId().toString());
        stateMachine.stopReactively().subscribe();
    }
}