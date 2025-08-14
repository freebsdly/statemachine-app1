package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        FileProcessingEvent event = context.getEvent();
        log.info("current event: {}", event);
        FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");
        CompletableFuture.runAsync(() -> run(context, fileInfo));

    }

    private void run(StateContext<FileProcessingState, FileProcessingEvent> context, FileInfo fileInfo) {
        log.info("开始执行文件上传...");
        try {
            // 执行文件上传逻辑
            boolean uploadSuccess = uploadFile(fileInfo);
            if (uploadSuccess) {
                // 文件上传成功，触发UPLOAD_SUCCESS事件
                Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.UPLOAD_SUCCESS)
                        .setHeader("fileInfo", fileInfo)
                        .build();
                // 修改为使用非阻塞方式发送事件并添加日志
                context.getStateMachine().sendEvent(Mono.just(message))
                        .subscribe();
            } else {
                Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.UPLOAD_FAILURE)
                        .setHeader("error", "文件上传失败")
                        .build();
                context.getStateMachine().sendEvent(Mono.just(message))
                        .subscribe();
            }
        } catch (Exception e) {
            log.error("文件上传过程中发生异常", e);
            Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.UPLOAD_FAILURE)
                    .setHeader("error", "文件上传失败: " + e.getMessage())
                    .build();
            context.getStateMachine().sendEvent(Mono.just(message))
                    .subscribe();
        }
    }


    private boolean uploadFile(FileInfo fileInfo) {
        // 实现文件上传逻辑
        log.info("正在上传文件: {}", fileInfo.getFileName());
        // 模拟上传过程
        try {
            Thread.sleep(2000); // 模拟上传耗时
            log.info("文件上传完成: {}", fileInfo.getFileName());
            return true;
        } catch (InterruptedException e) {
            log.error("文件上传被中断", e);
            return false;
        }
    }
}