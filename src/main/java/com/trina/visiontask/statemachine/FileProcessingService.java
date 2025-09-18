package com.trina.visiontask.statemachine;

import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.MQConfiguration;
import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.service.MessageProducer;
import com.trina.visiontask.service.ObjectStorageOptions;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import io.micrometer.core.annotation.Timed;
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
import java.util.concurrent.CompletableFuture;
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
    private final ObjectStorageOptions options;
    private final MQConfiguration taskLogConfig;

    public FileProcessingService(
            StateMachineManager stateMachineManager,
            MessageProducer messageProducer,
            TaskConfiguration taskConfiguration,
            TaskService taskService,
            @Qualifier("getCallbackWebClient") WebClient webClient,
            @Qualifier("objectStorageOptions") ObjectStorageOptions options,
            @Qualifier("taskLogConfiguration") MQConfiguration taskLogConfig
    ) {
        this.stateMachineManager = stateMachineManager;
        this.messageProducer = messageProducer;
        this.taskConfiguration = taskConfiguration;
        this.taskService = taskService;
        this.webClient = webClient;
        this.options = options;
        this.taskLogConfig = taskLogConfig;
    }

    @Timed(value = "file.process",
            description = "file process",
            percentiles = {0.5, 0.95},
            histogram = true
    )
    public void processFile(FileProcessingState initState, FileProcessingEvent event, TaskDTO dto)
            throws Exception {
        if (dto.getTaskId() == null) {
            throw new Exception("taskId cannot be null");
        }

        if (dto.getFileInfo().getFileName() == null) {
            throw new Exception("file name cannot be null");
        }
        // 这里使用builder创建状态机，以便从指定的初始状态开始，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = stateMachineManager.acquireStateMachine(initState, dto.getTaskId().toString());
        CountDownLatch completionLatch = new CountDownLatch(1);
        if (dto.getStartTime() == null) {
            dto.setStartTime(LocalDateTime.now());
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
                    taskInfo.getFileInfo().setStatus(target);
                    // 改成直接写入数据库
                    switch (target) {
                        case UPLOADED -> taskInfo.setMessage("file uploaded");
                        case PDF_CONVERTED -> taskInfo.setMessage("pdf converted");
                        case MARKDOWN_CONVERT_SUBMITTED -> taskInfo.setMessage("markdown convert submitted");
                        case MARKDOWN_CONVERTED -> taskInfo.setMessage("markdown converted");
                        case AI_SLICE_SUBMITTED -> taskInfo.setMessage("ai slice submitted");
                        case COMPLETED -> taskInfo.setMessage("completed");
                    }
                    final TaskDTO infoLog = taskService.saveOrUpdateTask(taskInfo);

                    switch (target) {
                        case UPLOADED -> messageProducer.sendToPdfConvertQueue(taskInfo);
                        case PDF_CONVERTED -> messageProducer.sendToMdConvertQueue(taskInfo);
                        case MARKDOWN_CONVERTED -> messageProducer.sendToAiSliceQueue(taskInfo);
                    }
                    if (taskLogConfig.isEnabled()) {
                        CompletableFuture.runAsync(() -> {
                            messageProducer.sendToTaskLogQueue(infoLog);
                        });
                    }
                    // 根据开关按需开启详细日志
                    if (taskConfiguration.isLogHistory()) {
                        CompletableFuture.runAsync(() -> {
                            messageProducer.sendToTaskLogKafka(infoLog);
                        });
                    }
                } else {
                    log.error("taskInfo is null");
                }

                // 检查是否到达可退出状态
                if (target == FileProcessingState.UPLOADED
                        || target == FileProcessingState.PDF_CONVERTED
                        || target == FileProcessingState.MARKDOWN_CONVERTED
                        || target == FileProcessingState.COMPLETED
                        || target == FileProcessingState.FAILED
                ) {
                    completionLatch.countDown();
                } else if (target == FileProcessingState.MARKDOWN_CONVERT_SUBMITTED) {
                    log.debug("waiting for markdown callback");
                } else if (target == FileProcessingState.AI_SLICE_SUBMITTED) {
                    log.debug("waiting for slice callback");
                }
            }
        };
        stateMachine.addStateListener(listener);
        stateMachine.startReactively().subscribe();
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader(taskConfiguration.getTaskInfoKey(), dto)
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
        stateMachineManager.releaseStateMachine(dto.getTaskId().toString());
        stateMachine.stopReactively().subscribe();
    }


    public void processCallback(CallbackInfo dto) throws Exception {
        String machineId = dto.getTaskId().toString();
        var stateMachine = stateMachineManager.getStateMachine(machineId);
        TaskDTO task = taskService.getTask(dto.getTaskId());
        if (stateMachine.isEmpty()) {
            // 状态机不在本地，转发到实际节点
            String callbackUrl = switch (dto.getStatus()) {
                case 1, 2 -> task.getMdCallbackUrl();
                case 3, 4 -> task.getSliceCallbackUrl();
                default -> null;
            };
            if (callbackUrl == null || callbackUrl.isEmpty()) {
                throw new Exception("callback url not found");
            }
            log.info("translation request to real node, call {}", callbackUrl);
            webClient.post()
                    .uri(callbackUrl)
                    // 这里注意taskId和itemId对应
                    .bodyValue(dto)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe();
            return;
        }
        var fileInfo = task.getFileInfo();
        var event = switch (dto.getStatus()) {
            case 1 -> {
                fileInfo.setMdPath(String.format("%s/%s", options.getEndpoint(), dto.getKey()));
                fileInfo.setOssMDKey(dto.getKey());
                yield FileProcessingEvent.MD_CONVERT_SUCCESS;
            }
            case 2 -> FileProcessingEvent.MD_CONVERT_FAILURE;
            case 3 -> FileProcessingEvent.AI_SLICE_SUCCESS;
            case 4 -> FileProcessingEvent.AI_SLICE_FAILURE;
            default -> throw new Exception(String.format("callback status %d unsupported", dto.getStatus()));
        };
        task.setFileInfo(fileInfo);
        var message = MessageBuilder.withPayload(event)
                .setHeader(taskConfiguration.getTaskInfoKey(), task)
                .build();

        stateMachine.get().sendEvent(Mono.just(message)).subscribe();
    }
}