package com.trina.visiontask.biz;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.trina.visiontask.converter.DocumentConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class PdfConvertAction implements Action<FileProcessingState, FileProcessingEvent> {
    private static final Logger log = LoggerFactory.getLogger(PdfConvertAction.class);
    private final ObjectStorageService objectStorageService;
    private final DocumentConverter documentConverter;
    private final String taskInfoKey;
    private final long timeout;

    public PdfConvertAction(
            @Qualifier("PDFDocumentConverter") DocumentConverter documentConverter,
            ObjectStorageService objectStorageService,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("pdfConvertTaskTimeout") long timeout
    ) {
        this.documentConverter = documentConverter;
        this.objectStorageService = objectStorageService;
        this.taskInfoKey = taskInfoKey;
        this.timeout = timeout;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
            try {
                convertToPdf(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                        .setHeader(taskInfoKey, taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_FAILURE)
                        .setHeader("error", e.getMessage())
                        .setHeader(taskInfoKey, taskInfo)
                        .build();

            }
            context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
        }).orTimeout(timeout, TimeUnit.SECONDS);
        ;
    }

    public void convertToPdf(TaskInfo taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        taskInfo.setStartTime(LocalDateTime.now());
        FileInfo fileInfo = taskInfo.getFileInfo();
        log.info("converting pdf: {}", fileInfo.getFileName());
        Optional<OSSObject> download = objectStorageService.download(fileInfo.getOssFileKey()).blockOptional();
        if (download.isEmpty()) {
            throw new Exception("file not found");
        }
        OSSObject ossObject = download.get();
        long contentLength = ossObject.getObjectMetadata().getContentLength();
        String uploadName = String.format("%s.%s", UUID.randomUUID(), "pdf");
        Flux<DataBuffer> convert = documentConverter.convert(
                ossObject.getObjectContent(),
                uploadName,
                contentLength,
                null);
        Optional<CompleteMultipartUploadResult> uploadResult = objectStorageService
                .uploadFlux(uploadName, convert)
                .blockOptional();
        if (uploadResult.isEmpty()) {
            log.error("upload pdf {} failed", fileInfo.getFileName());
            throw new Exception("upload pdf failed");
        }
        CompleteMultipartUploadResult result = uploadResult.get();
        fileInfo.setOssPDFKey(result.getKey());
        fileInfo.setPdfPath(result.getLocation());
        taskInfo.setFileInfo(fileInfo);
        taskInfo.setEndTime(LocalDateTime.now());
        log.info("convert pdf {} finished", fileInfo.getFileName());
    }
}

