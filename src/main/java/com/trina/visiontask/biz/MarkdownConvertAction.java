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
public class MarkdownConvertAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final MessageProducer messageProducer;

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            log.info("start converting markdown");
            Message<FileProcessingEvent> message;
            try {
                // 获取文件信息
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");

                convertToMarkdown(fileInfo);

                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUCCESS)
                        .setHeader("fileInfo", fileInfo)
                        .build();
            } catch (Exception e) {
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_FAILURE)
                        .setHeader("error", "convert markdown failed")
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
        });
    }

    private void convertToMarkdown(FileInfo fileInfo) throws Exception {
        if (fileInfo == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        log.info("converting markdown {}", fileInfo.getFileName());
        Thread.sleep(5000); // 模拟上传耗时
        log.info("convert markdown {} finished", fileInfo.getFileName());
        // TODO: 更新文件状态
        messageProducer.sendToAiSliceQueue(fileInfo);
    }
}
