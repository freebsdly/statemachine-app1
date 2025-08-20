package com.trina.visiontask.biz;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@AllArgsConstructor(onConstructor_ = @Autowired)
public class FileUploadAction implements Action<FileProcessingState, FileProcessingEvent>
{

    private final MessageProducer messageProducer;

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context)
    {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            try {
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");
                // 执行文件上传逻辑
                boolean uploadSuccess = uploadFile(fileInfo);

                if (uploadSuccess) {
                    // 文件上传成功，触发UPLOAD_SUCCESS事件
                    message = MessageBuilder
                            .withPayload(FileProcessingEvent.UPLOAD_SUCCESS)
                            .setHeader("fileInfo", fileInfo)
                            .build();
                    // 修改为使用非阻塞方式发送事件并添加日志
                } else {
                    message = MessageBuilder
                            .withPayload(FileProcessingEvent.UPLOAD_FAILURE)
                            .setHeader("error", "upload file failed")
                            .build();
                }
                context.getStateMachine().sendEvent(Mono.just(message))
                        .blockLast();
            } catch (Exception e) {
                log.error("upload file failed", e);
                message = MessageBuilder.withPayload(FileProcessingEvent.UPLOAD_FAILURE)
                        .setHeader("error", "upload file failed")
                        .build();
                context.getStateMachine().sendEvent(Mono.just(message))
                        .blockLast();
            }
        });

    }

    private boolean uploadFile(FileInfo fileInfo) throws Exception
    {
        if (fileInfo == null) {
            log.error("file info is null");
            return false;
        }
        // 实现文件上传逻辑
        log.info("start uploading file {}", fileInfo.getFileName());
        Thread.sleep(1000); // 模拟上传耗时
        log.info("upload file {} finished", fileInfo.getFileName());
        // TODO: 更新文件状态
        messageProducer.sendToPdfConvertQueue(fileInfo);
        return true;
    }
}