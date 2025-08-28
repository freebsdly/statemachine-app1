package com.trina.visiontask.statemachine;

import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.converter.AlgRequestDTO;
import com.trina.visiontask.converter.AlgResponseDTO;
import com.trina.visiontask.converter.ConverterOptions;
import com.trina.visiontask.converter.DocumentConverter;
import com.trina.visiontask.service.FileDTO;
import com.trina.visiontask.service.TaskDTO;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class AiSliceSubmitAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final DocumentConverter aiSliceConverter;
    private final ConverterOptions converterOptions;
    private final TaskConfiguration taskConfiguration;

    public AiSliceSubmitAction(
            @Qualifier("aiSliceConverter") DocumentConverter aiSliceConverter,
            @Qualifier("aiSliceConverterOptions") ConverterOptions converterOptions,
            TaskConfiguration taskConfiguration
    ) {
        this.aiSliceConverter = aiSliceConverter;
        this.converterOptions = converterOptions;
        this.taskConfiguration = taskConfiguration;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskDTO taskInfo = (TaskDTO) context.getMessage().getHeaders().get(taskConfiguration.getTaskInfoKey());
            try {
                submitAiSliceRequest(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.AI_SLICE_SUBMIT_SUCCESS)
                        .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder.withPayload(FileProcessingEvent.AI_SLICE_SUBMIT_FAILURE)
                        .setHeader("error", "process ai slice failed")
                        .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                        .build();
            }
            context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
        }).orTimeout(taskConfiguration.getAiSliceTaskTimeout(), TimeUnit.SECONDS);
        ;
    }

    @Timed("ai-slice-submit")
    private void submitAiSliceRequest(TaskDTO taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        taskInfo.setStartTime(LocalDateTime.now());
        FileDTO fileInfo = taskInfo.getFileInfo();
        log.info("submitting ai slice request, {}", fileInfo.getFileName());
        AlgRequestDTO options = new AlgRequestDTO(fileInfo.getFileId().toString(), fileInfo.getOssMDKey(),
                System.currentTimeMillis(), converterOptions.getEnvId());
        Optional<AlgResponseDTO> result = aiSliceConverter.convert(
                options, AlgResponseDTO.class, null).blockOptional();
        if (result.isEmpty() || !result.get().isSuccess()) {
            throw new Exception("submit ai slice request failed");
        }
        taskInfo.setEndTime(LocalDateTime.now());
        log.info("{} ai slice request submitted", fileInfo.getFileName());
    }
}

