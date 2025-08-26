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
public class AiSliceSubmitAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final String taskInfoKey;
    private final long timeout;

    public AiSliceSubmitAction(
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("aiSliceTaskTimeout") long timeout
    ) {
        this.taskInfoKey = taskInfoKey;
        this.timeout = timeout;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                submitAiSliceRequest(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.AI_SLICE_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder.withPayload(FileProcessingEvent.AI_SLICE_FAILURE)
                        .setHeader("error", "process ai slice failed")
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
        }).orTimeout(timeout, TimeUnit.SECONDS);
        ;
    }

    private void submitAiSliceRequest(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            throw new Exception("file info is null");
        }
        FileInfo fileInfo = taskInfo.getFileInfo();
        taskInfo.setStartTime(LocalDateTime.now());
        log.info("processing ai slice: {}", fileInfo.getFileName());
        Thread.sleep(4000); // 模拟上传耗时
        taskInfo.setEndTime(LocalDateTime.now());
        log.info("ai slice {} finished", fileInfo.getFileName());
    }
}

