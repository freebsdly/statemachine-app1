package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class FileUploadAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final MessageProducer messageProducer;
    private final String taskInfoKey;

    public FileUploadAction(MessageProducer messageProducer, @Qualifier("taskInfoKey") String taskInfoKey) {
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                taskInfo = uploadFile(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.UPLOAD_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                taskInfo.setMessage(e.getMessage());
                message = MessageBuilder.withPayload(FileProcessingEvent.UPLOAD_FAILURE)
                        .setHeader("error", "upload file failed")
                        .build();

            }
            context.getStateMachine().sendEvent(Mono.just(message))
                    .blockLast();
        });
    }

    private TaskInfo uploadFile(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            throw new Exception("file info is null");
        }
        // 实现文件上传逻辑
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("start uploading file {}", fileInfo.getFileName());
        Thread.sleep(1000); // 模拟上传耗时
        log.info("upload file {} finished", fileInfo.getFileName());
        // TODO: 更新文件状态
        messageProducer.sendToPdfConvertQueue(taskInfo);
        return taskInfo;
    }
}