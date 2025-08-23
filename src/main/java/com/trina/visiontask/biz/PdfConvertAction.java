package com.trina.visiontask.biz;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PdfConvertAction implements Action<FileProcessingState, FileProcessingEvent>
{

    private final ObjectStorageService objectStorageService;
    private final DocumentConverter documentConverter;
    private final MessageProducer messageProducer;
    private final String taskInfoKey;

    public PdfConvertAction(
            @Qualifier("PDFDocumentConverter") DocumentConverter documentConverter,
            ObjectStorageService objectStorageService,
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey
                           )
    {
        this.documentConverter = documentConverter;
        this.objectStorageService = objectStorageService;
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context)
    {
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
        }).orTimeout(300, TimeUnit.SECONDS);
        ;
    }

    private TaskInfo convertToPdf(TaskInfo taskInfo) throws Exception
    {
        if (taskInfo == null || taskInfo.getFileInfo() == null) {
            log.error("file info is null");
            throw new Exception("file info is null");
        }
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
        log.info("convert pdf {} finished", fileInfo.getFileName());
        taskInfo.setFileInfo(fileInfo);
        messageProducer.sendToMdConvertQueue(taskInfo);
        return taskInfo;
    }
}

