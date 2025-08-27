package com.trina.visiontask.biz;

import com.trina.visiontask.converter.MarkdownDocumentConverter;
import lombok.Data;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MdConvertSubmitAction implements Action<FileProcessingState, FileProcessingEvent> {
    private static final Logger log = LoggerFactory.getLogger(MdConvertSubmitAction.class);
    private final MarkdownDocumentConverter markdownDocumentConverter;
    private final String taskInfoKey;
    private final long timeout;

    public MdConvertSubmitAction(
            @Qualifier("mdConverter") MarkdownDocumentConverter markdownDocumentConverter,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("mdConvertTaskTimeout") long timeout
    ) {
        this.markdownDocumentConverter = markdownDocumentConverter;
        this.taskInfoKey = taskInfoKey;
        this.timeout = timeout;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                submitMarkdownConvertRequest(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUBMIT_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUBMIT_FAILURE)
                        .setHeader("error", "submit convert markdown request failed")
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
        }).orTimeout(timeout, TimeUnit.SECONDS);
    }

    private void submitMarkdownConvertRequest(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        taskInfo.setStartTime(LocalDateTime.now());
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("submitting md {} convert request", fileInfo.getFileName());
        MarkdownConvertDTO options = new MarkdownConvertDTO();
        options.setKey(fileInfo.getOssPDFKey());
        Optional<MarkDownConvertSubmittedDTO> result = markdownDocumentConverter.convert(
                options, MarkDownConvertSubmittedDTO.class, null).blockOptional();
        if (result.isEmpty() || !result.get().success) {
            throw new Exception("submit md convert request failed");
        }
        taskInfo.setEndTime(LocalDateTime.now());
        log.info("md {} convert request submitted", fileInfo.getFileName());
    }

    @Data
    public static class MarkDownConvertSubmittedDTO {
        private boolean success;
        private String errMsg;
        private String errCode;
    }

    @Data
    public static class MarkdownConvertDTO {
        private String itemId;
        private String key;
        private String envId;
    }
}
