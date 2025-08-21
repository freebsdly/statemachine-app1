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

@Slf4j
@Component
public class MarkdownConvertAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final MessageProducer messageProducer;
    private final String taskInfoKey;

    public MarkdownConvertAction(MessageProducer messageProducer, @Qualifier("taskInfoKey") String taskInfoKey) {
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            log.info("start converting markdown");
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                taskInfo = convertToMarkdown(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                taskInfo.setMessage(e.getMessage());
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_FAILURE)
                        .setHeader("error", "convert markdown failed")
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
        });
    }

    private TaskInfo convertToMarkdown(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("converting markdown {}", fileInfo.getFileName());
        Thread.sleep(5000); // 模拟上传耗时
        log.info("convert markdown {} finished", fileInfo.getFileName());
        // TODO: 更新文件状态
        messageProducer.sendToAiSliceQueue(taskInfo);
        return taskInfo;
    }
}
