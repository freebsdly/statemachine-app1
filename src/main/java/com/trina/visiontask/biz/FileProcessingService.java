package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileProcessingService
{

    private final FileStateMachineService fileStateMachineService;
    // 注入各个步骤的处理器
    private final MessageProducer messageProducer;

    private final String taskInfoKey;

    private final long waitTimeout;

    public FileProcessingService(
            FileStateMachineService fileStateMachineService,
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("waitTimeout") long timeout)
    {
        this.fileStateMachineService = fileStateMachineService;
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
        this.waitTimeout = timeout;
    }

    public void processFile(FileProcessingState initState, FileProcessingEvent event, TaskInfo taskInfo)
            throws Exception
    {
        // 这里使用builder创建状态机，以便从指定的初始状态开始，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine =
                fileStateMachineService.acquireStateMachine(taskInfo.getId().toString(), initState, true);
        CountDownLatch completionLatch = new CountDownLatch(1);
        if (taskInfo.getStartTime() == null) {
            taskInfo.setStartTime(LocalDateTime.now());
        }
        StateMachineListenerAdapter<FileProcessingState, FileProcessingEvent> listener = new StateMachineListenerAdapter<>()
        {
            private StateContext<FileProcessingState, FileProcessingEvent> context;

            @Override
            public void stateContext(StateContext<FileProcessingState, FileProcessingEvent> stateContext)
            {
                this.context = stateContext;
            }

            @Override
            public void transitionEnded(Transition<FileProcessingState, FileProcessingEvent> transition)
            {
                FileProcessingState source = transition.getSource().getId();
                FileProcessingState target = transition.getTarget().getId();
                FileProcessingEvent event = transition.getTrigger().getEvent();
                TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);

                if (taskInfo != null) {
                    taskInfo.setPreviousState(source);
                    taskInfo.setCurrentState(target);
                    taskInfo.setEvent(event);
                    if (taskInfo.getEndTime() == null) {
                        taskInfo.setEndTime(LocalDateTime.now());
                    }
                }
                log.info("State changed from {} to {}, triggered by {}", source, target, event);
                logTaskInfo(taskInfo);

                // 检查是否到达最终状态
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

            private void logTaskInfo(TaskInfo taskInfo)
            {
                log.info("TaskInfo: {}", taskInfo);
                messageProducer.sendToTaskLogQueue(taskInfo);
            }
        };
        stateMachine.addStateListener(listener);
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader(taskInfoKey, taskInfo)
                .build();
        // 发送初始事件，开始文件处理流程
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        // 等待处理完成
        boolean finished = completionLatch.await(waitTimeout, TimeUnit.SECONDS);
        fileStateMachineService.releaseStateMachine(taskInfo.getId().toString());
        if (!finished) {
            throw new Exception("state machine completion latch timeout");
        }
    }
}