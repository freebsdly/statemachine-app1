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
public class PdfConvertAction implements Action<FileProcessingState, FileProcessingEvent> {

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            log.info("开始执行PDF转换...");
            try {
                // 获取文件信息
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");

                boolean convertSuccess = convertToPdf(fileInfo);

                if (convertSuccess) {
                    Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                            .setHeader("fileInfo", fileInfo)
                            .build();
                    context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
                } else {
                    Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.PDF_CONVERT_FAILURE)
                            .setHeader("error", "pdf转换失败")
                            .build();
                    context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
                }
            } catch (Exception e) {
                Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.PDF_CONVERT_FAILURE)
                        .setHeader("error", "pdf转换失败")
                        .build();
                context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
            }
        });
    }

    private boolean convertToPdf(FileInfo fileInfo) {
        log.info("正在转换PDF文件: {}", fileInfo.getFileName());
        try {
            Thread.sleep(2000); // 模拟上传耗时
            log.info("转换PDF文件完成: {}", fileInfo.getFileName());
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}

