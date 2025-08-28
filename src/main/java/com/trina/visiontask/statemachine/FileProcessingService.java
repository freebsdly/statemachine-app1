package com.trina.visiontask.statemachine;

import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.service.MessageProducer;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class FileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    private final StateMachineManager stateMachineManager;
    private final MessageProducer messageProducer;
    private final TaskConfiguration taskConfiguration;
    private final TaskService taskService;
    private final WebClient webClient;

    public FileProcessingService(
            StateMachineManager stateMachineManager,
            MessageProducer messageProducer,
            TaskConfiguration taskConfiguration,
            TaskService taskService,
            @Qualifier("getCallbackWebClient") WebClient webClient) {
        this.stateMachineManager = stateMachineManager;
        this.messageProducer = messageProducer;
        this.taskConfiguration = taskConfiguration;
        this.taskService = taskService;
        this.webClient = webClient;
    }

    public void processFile(FileProcessingState initState, FileProcessingEvent event, TaskDTO taskInfo)
            throws Exception {
        if (taskInfo.getTaskId() == null) {
            throw new Exception("taskId cannot be null");
        }

        if (taskInfo.getFileInfo().getFileName() == null) {
            throw new Exception("file name cannot be null");
        }
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
                log.debug("State changed from {} to {}, triggered by {}", source, target, event);
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
                    log.debug("waiting for markdown or slice callback");
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
            // 这里可能会发生等待回调超时，不重试，通过定时任务扫描特定状态在处理
            log.warn("File processing timed out");
        }
        stateMachineManager.releaseStateMachine(taskInfo.getTaskId().toString());
        stateMachine.stopReactively().subscribe();
    }

    public void processCallback(CallbackInfo dto) throws Exception {
        String machineId = dto.getTaskId();
        var stateMachine = stateMachineManager.getStateMachine(machineId);
        TaskDTO task = taskService.getTask(UUID.fromString(dto.getTaskId()));
        if (stateMachine.isEmpty()) {
            // 状态机不在本地，转发到实际节点
            String mdCallbackUrl = task.getMdCallbackUrl();
            if (mdCallbackUrl == null || mdCallbackUrl.isEmpty()) {
                throw new Exception("md callback url not found");
            }
            log.info("translation request to real node, call {}", mdCallbackUrl);
            webClient.post()
                    .uri(mdCallbackUrl)
                    .bodyValue(dto)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe();
            return;
        }
        var event = switch (dto.getStatus()) {
            case 1 -> FileProcessingEvent.MD_CONVERT_SUCCESS;
            case 2 -> FileProcessingEvent.MD_CONVERT_FAILURE;
            case 3 -> FileProcessingEvent.AI_SLICE_SUCCESS;
            case 4 -> FileProcessingEvent.AI_SLICE_FAILURE;
            default -> throw new Exception(String.format("callback status %d unsupported", dto.getStatus()));
        };

        var message = MessageBuilder.withPayload(event)
                .setHeader(taskConfiguration.getTaskInfoKey(), task)
                .build();

        stateMachine.get().sendEvent(Mono.just(message)).subscribe();
    }
}