package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MdConvertCallbackAction implements Action<FileProcessingState, FileProcessingEvent> {
    private final MessageProducer messageProducer;
    private final String taskInfoKey;
    private final long timeout;

    public MdConvertCallbackAction(
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("mdConvertTaskTimeout") long timeout
    ) {
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
        this.timeout = timeout;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                convertToMarkdownCallback(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_FAILURE)
                        .setHeader("error", "convert markdown callback failed")
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
        }).orTimeout(timeout, TimeUnit.SECONDS);
    }

    private void convertToMarkdownCallback(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        if (taskInfo.getEndTime() == null) {
            taskInfo.setEndTime(LocalDateTime.now());
        }
        messageProducer.sendToAiSliceQueue(taskInfo);
    }
}
