package com.trina.visiontask.statemachine;

import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.converter.AlgRequestDTO;
import com.trina.visiontask.converter.AlgResponseDTO;
import com.trina.visiontask.converter.ConverterOptions;
import com.trina.visiontask.converter.MarkdownDocumentConverter;
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
    private final ConverterOptions converterOptions;
    private final TaskConfiguration taskConfiguration;

    public MdConvertSubmitAction(
            @Qualifier("mdConverter") MarkdownDocumentConverter markdownDocumentConverter,
            @Qualifier("mdConverterOptions") ConverterOptions converterOptions,
            TaskConfiguration taskConfiguration
    ) {
        this.markdownDocumentConverter = markdownDocumentConverter;
        this.converterOptions = converterOptions;
        this.taskConfiguration = taskConfiguration;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskConfiguration.getTaskInfoKey());
            try {
                submitMarkdownConvertRequest(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUBMIT_SUCCESS)
                        .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.MD_CONVERT_SUBMIT_FAILURE)
                        .setHeader("error", "submit convert markdown request failed")
                        .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
        }).orTimeout(taskConfiguration.getMdConvertTaskTimeout(), TimeUnit.SECONDS);
    }

    private void submitMarkdownConvertRequest(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        taskInfo.setStartTime(LocalDateTime.now());
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("submitting md {} convert request", fileInfo.getFileName());
        AlgRequestDTO options = new AlgRequestDTO(fileInfo.getFileId().toString(), fileInfo.getOssPDFKey(),
                null, converterOptions.getEnvId());
        Optional<AlgResponseDTO> result = markdownDocumentConverter.convert(
                options, AlgResponseDTO.class, null).blockOptional();
        if (result.isEmpty() || !result.get().isSuccess()) {
            throw new Exception("submit md convert request failed");
        }
        taskInfo.setEndTime(LocalDateTime.now());
        log.info(" {} md convert request submitted", fileInfo.getFileName());
    }
}
