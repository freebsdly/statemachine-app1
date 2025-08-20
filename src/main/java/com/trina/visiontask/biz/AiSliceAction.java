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
public class AiSliceAction implements Action<FileProcessingState, FileProcessingEvent>
{

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context)
    {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            try {
                // 获取文件信息
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");

                // 执行文件上传逻辑
                boolean uploadSuccess = processWithAi(fileInfo);

                if (uploadSuccess) {
                    // 文件上传成功，触发UPLOAD_SUCCESS事件
                    message = MessageBuilder
                            .withPayload(FileProcessingEvent.AI_SLICE_SUCCESS)
                            .setHeader("fileInfo", fileInfo)
                            .build();
                } else {
                    message = MessageBuilder
                            .withPayload(FileProcessingEvent.AI_SLICE_FAILURE)
                            .setHeader("error", "process ai slice failed")
                            .build();
                }
                context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
            } catch (Exception e) {
                message = MessageBuilder.withPayload(FileProcessingEvent.AI_SLICE_FAILURE)
                        .setHeader("error", "process ai slice failed")
                        .build();
                context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
            }
        });
    }

    private boolean processWithAi(FileInfo fileInfo) throws Exception
    {
        if (fileInfo == null) {
            log.error("file info is null");
            return false;
        }
        log.info("processing ai slice: {}", fileInfo.getFileName());
        Thread.sleep(4000); // 模拟上传耗时
        log.info("ai slice {} finished", fileInfo.getFileName());
        return true;
    }
}

