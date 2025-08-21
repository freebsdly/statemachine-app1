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
public class PdfConvertAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final MessageProducer messageProducer;

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            log.info("start converting pdf");
            Message<FileProcessingEvent> message;
            try {
                // 获取文件信息
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");

                convertToPdf(fileInfo);

                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                        .setHeader("fileInfo", fileInfo)
                        .build();
            } catch (Exception e) {
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_FAILURE)
                        .setHeader("error", "convert pdf failed")
                        .build();

            }
            context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
        });
    }

    private void convertToPdf(FileInfo fileInfo) throws Exception {
        if (fileInfo == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        log.info("converting pdf: {}", fileInfo.getFileName());
        Thread.sleep(2000); // 模拟上传耗时
        log.info("convert pdf {} finished", fileInfo.getFileName());
        // TODO: 更新文件状态
        messageProducer.sendToMdConvertQueue(fileInfo);
    }
}

