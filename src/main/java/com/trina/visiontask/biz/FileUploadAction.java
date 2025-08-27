package com.trina.visiontask.biz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


@Component
public class FileUploadAction implements Action<FileProcessingState, FileProcessingEvent> {

    private static final Logger log = LoggerFactory.getLogger(FileUploadAction.class);

    private final String taskInfoKey;
    private final long timeout;

    public FileUploadAction(
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("uploadTaskTimeout") long timeout) {
        this.taskInfoKey = taskInfoKey;
        this.timeout = timeout;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                uploadFile(taskInfo);
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
            context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
        }).orTimeout(timeout, TimeUnit.SECONDS);
    }

    // 这里只处理从网络存储获取文件并上传到对象存储的逻辑
    private void uploadFile(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            throw new Exception("file info is null");
        }
        // TODO: 实现文件上传逻辑
        FileInfo fileInfo = taskInfo.getFileInfo();
        taskInfo.setStartTime(LocalDateTime.now());
        log.info("start uploading file {}", fileInfo.getFileName());
        // 增加延时让状态转换完成
        Thread.sleep(500);
        taskInfo.setEndTime(LocalDateTime.now());
        log.info("upload file {} finished", fileInfo.getFileName());
    }
}