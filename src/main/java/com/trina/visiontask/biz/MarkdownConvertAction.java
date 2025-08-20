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
            log.info("开始转换markdown...");

            try {
                // 获取文件信息
                FileInfo fileInfo = (FileInfo) context.getMessage().getHeaders().get("fileInfo");

                boolean convertSuccess = convertToMarkdown(fileInfo);

                if (convertSuccess) {
                    // 文件上传成功，触发UPLOAD_SUCCESS事件
                    Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.MD_CONVERT_SUCCESS)
                            .setHeader("fileInfo", fileInfo)
                            .build();
                    context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
                } else {
                    Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.MD_CONVERT_FAILURE)
                            .setHeader("error", "转换markdown失败")
                            .build();
                    context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
                }
            } catch (Exception e) {
                Message<FileProcessingEvent> message = MessageBuilder.withPayload(FileProcessingEvent.MD_CONVERT_FAILURE)
                        .setHeader("error", "转换markdown失败")
                        .build();
                context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
            }
        });
    }

    private boolean convertToMarkdown(FileInfo fileInfo) {
        log.info("正在转换Markdown: {}", fileInfo.getFileName());
        try {
            Thread.sleep(5000); // 模拟上传耗时
            log.info("转换Markdown完成: {}", fileInfo.getFileName());
            // TODO: 更新文件状态
            messageProducer.sendToAiSliceQueue(fileInfo);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
