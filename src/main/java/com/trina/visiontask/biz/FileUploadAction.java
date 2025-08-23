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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FileUploadAction implements Action<FileProcessingState, FileProcessingEvent>
{

    private final ObjectStorageService objectStorageService;
    private final MessageProducer messageProducer;
    private final String taskInfoKey;
    private final long timeout;

    public FileUploadAction(
            ObjectStorageService objectStorageService,
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("uploadTaskTimeout") long timeout)
    {
        this.objectStorageService = objectStorageService;
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
        this.timeout = timeout;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context)
    {
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
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder.withPayload(FileProcessingEvent.UPLOAD_FAILURE)
                        .setHeader("error", "upload file failed")
                        .setHeader(taskInfoKey, taskInfo)
                        .build();

            }
            context.getStateMachine().sendEvent(Mono.just(message))
                    .blockLast();
        }).orTimeout(timeout, TimeUnit.SECONDS);
    }

    // 这里只处理从网络存储获取文件并上传到对象存储的逻辑
    private TaskInfo uploadFile(TaskInfo taskInfo) throws Exception
    {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            throw new Exception("file info is null");
        }
        // TODO: 实现文件上传逻辑
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("start uploading file {}", fileInfo.getFileName());

        log.info("upload file {} finished", fileInfo.getFileName());
        taskInfo.setCurrentState(FileProcessingState.UPLOADED);
        messageProducer.sendToPdfConvertQueue(taskInfo);
        return taskInfo;
    }
}