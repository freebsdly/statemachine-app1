package com.trina.visiontask.statemachine;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.converter.DocumentConverter;
import com.trina.visiontask.service.FileDTO;
import com.trina.visiontask.service.ObjectStorageService;
import com.trina.visiontask.service.TaskDTO;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class PdfConvertAction implements Action<FileProcessingState, FileProcessingEvent> {
    private static final Logger log = LoggerFactory.getLogger(PdfConvertAction.class);
    private final ObjectStorageService objectStorageService;
    private final DocumentConverter documentConverter;
    private final TaskConfiguration taskConfiguration;

    public PdfConvertAction(
            @Qualifier("PDFDocumentConverter") DocumentConverter documentConverter,
            ObjectStorageService objectStorageService,
            TaskConfiguration taskConfiguration
    ) {
        this.documentConverter = documentConverter;
        this.objectStorageService = objectStorageService;
        this.taskConfiguration = taskConfiguration;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        CompletableFuture.runAsync(() -> {
            Message<FileProcessingEvent> message;
            TaskDTO taskInfo = (TaskDTO) context.getMessage().getHeaders().get(taskConfiguration.getTaskInfoKey());
            try {
                convertToPdf(taskInfo);
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                        .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                        .build();
            } catch (Exception e) {
                if (taskInfo != null) {
                    taskInfo.setMessage(e.getMessage());
                }
                message = MessageBuilder
                        .withPayload(FileProcessingEvent.PDF_CONVERT_FAILURE)
                        .setHeader("error", e.getMessage())
                        .setHeader(taskConfiguration.getTaskInfoKey(), taskInfo)
                        .build();

            }
            context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
        }).orTimeout(taskConfiguration.getPdfConvertTaskTimeout(), TimeUnit.SECONDS);
    }

    public void convertToPdf(TaskDTO taskInfo) throws Exception {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
        taskInfo.setStartTime(LocalDateTime.now());
        FileDTO fileInfo = taskInfo.getFileInfo();
        if (taskInfo.checkSupportedFileType()) {
            log.info("converting pdf: {}", fileInfo.getFileName());
            Optional<OSSObject> download = objectStorageService.download(fileInfo.getOssFileKey()).blockOptional();
            if (download.isEmpty()) {
                throw new Exception("file not found");
            }
            OSSObject ossObject = download.get();
            long contentLength = ossObject.getObjectMetadata().getContentLength();

            String ossFileName = fileInfo.getOssFileKey();
            String fileNamePrefix;
            int i = ossFileName.lastIndexOf('.');
            if (i > 0) {
                fileNamePrefix = ossFileName.substring(0, i);
            } else {
                fileNamePrefix = ossFileName;
            }
            String uploadName = String.format("%s.%s", fileNamePrefix, "pdf");
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
            log.info("convert pdf {} finished", fileInfo.getFileName());
        } else {
            Thread.sleep(500);
            log.info("file type is {}, do not need to be converted to pdf", fileInfo.getFileType());
            fileInfo.setOssPDFKey(fileInfo.getOssFileKey());
            fileInfo.setPdfPath(fileInfo.getFilePath());
            taskInfo.setFileInfo(fileInfo);
        }
        taskInfo.setEndTime(LocalDateTime.now());
    }
}

