package com.trina.visiontask.biz;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.trina.visiontask.converter.DocumentConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class PdfConvertAction implements Action<FileProcessingState, FileProcessingEvent> {

    private final ObjectStorageService objectStorageService;
    private final DocumentConverter documentConverter;
    private final MessageProducer messageProducer;
    private final String taskInfoKey;

    public PdfConvertAction(
            @Qualifier("PDFDocumentConverter") DocumentConverter documentConverter,
            ObjectStorageService objectStorageService,
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey
    ) {
        this.documentConverter = documentConverter;
        this.objectStorageService = objectStorageService;
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            log.info("start converting pdf");
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                taskInfo = convertToPdf(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                taskInfo.setMessage(e.getMessage());
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_FAILURE)
                        .setHeader("error", e.getMessage())
                        .build();

            }
            context.getStateMachine().sendEvent(Mono.just(message)).blockLast();
        });
    }

    private TaskInfo convertToPdf(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("converting pdf: {}", fileInfo.getFileName());
        Flux<DataBuffer> dataBufferFlux = objectStorageService.downloadFlux(fileInfo.getOssFileKey());
        Flux<DataBuffer> convert = documentConverter.convert(dataBufferFlux, fileInfo.getMimeType(), null);
        Mono<CompleteMultipartUploadResult> resultMono = objectStorageService.uploadFlux(UUID.randomUUID().toString(), convert);
        CompleteMultipartUploadResult result = resultMono.block();
        if (result == null) {
            log.error("convert pdf {} failed", fileInfo.getFileName());
            throw new Exception("convert pdf failed");
        }
        fileInfo.setOssPDFKey(result.getKey());
        fileInfo.setPdfPath(result.getLocation());
        log.info("convert pdf {} finished", fileInfo.getFileName());
        taskInfo.setFileInfo(fileInfo);
        messageProducer.sendToMdConvertQueue(taskInfo);
        return taskInfo;
    }
}

