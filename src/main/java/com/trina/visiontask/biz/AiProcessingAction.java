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
public class AiProcessingAction implements Action<FileProcessingState, FileProcessingEvent> {

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            log.info("开始执行AI切片...");

            try {
                // 获取文件信息
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");

                // 执行文件上传逻辑
                boolean uploadSuccess = processWithAi(fileInfo);

                if (uploadSuccess) {
                    // 文件上传成功，触发UPLOAD_SUCCESS事件
                    Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.AI_PROCESS_SUCCESS)
                            .setHeader("fileInfo", fileInfo)
                            .build();
                    context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
                } else {
                    Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.AI_PROCESS_FAILURE)
                            .setHeader("error", "执行AI切片失败")
                            .build();
                    context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
                }
            } catch (Exception e) {
                Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.AI_PROCESS_FAILURE)
                        .setHeader("error", "执行AI切片失败")
                        .build();
                context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
            }
        });
    }

    private boolean processWithAi(FileInfo fileInfo) {
        log.info("正在执行AI切片: {}", fileInfo.getFileName());
        try {
            Thread.sleep(2000); // 模拟上传耗时
            log.info("执行AI切片完成: {}", fileInfo.getFileName());
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}

