package com.trina.visiontask.api;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.trina.visiontask.biz.*;
import com.trina.visiontask.converter.DocumentConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class Api implements ApiDoc {

    private final MessageProducer messageProducer;
    private final ObjectStorageService objectStorageService;
    private final DocumentConverter pdfDocumentConverter;

    public Api(MessageProducer messageProducer,
               ObjectStorageService objectStorageService,
               @Qualifier("PDFDocumentConverter") DocumentConverter pdfDocumentConverter
    ) {
        this.messageProducer = messageProducer;
        this.objectStorageService = objectStorageService;
        this.pdfDocumentConverter = pdfDocumentConverter;
    }


    @Override
    @PostMapping("/process-file")
    public ApiBody<String> processFile(@RequestBody TaskInfo info) throws Exception {
        // TODO: 根据初始状态和事件判断发送到哪个处理队列
        messageProducer.sendToUploadQueue(info);
        return ApiBody.success();
    }

    @Override
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiBody<TaskInfo> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        // 构造TaskInfo对象
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setId(UUID.randomUUID());
        taskInfo.setStartTime(LocalDateTime.now());
        String suffix = null;
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("file name is empty");
        }
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            suffix = originalFilename.substring(i + 1);
        }
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(UUID.randomUUID());
        fileInfo.setFileName(originalFilename);
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setFileType(suffix);
        String uploadName = String.format("%s.%s", fileInfo.getId(), fileInfo.getFileType());
        Mono<CompleteMultipartUploadResult> upload = objectStorageService.upload(uploadName, file.getInputStream());
        CompleteMultipartUploadResult result = upload.block();
        if (result == null) {
            log.error("upload file {} failed", fileInfo.getFileName());
            throw new Exception("upload file failed");
        }
        fileInfo.setOssFileKey(result.getKey());
        fileInfo.setFilePath(result.getLocation());
        fileInfo.setFileSize(file.getSize());
        taskInfo.setFileInfo(fileInfo);
        taskInfo.setEndTime(LocalDateTime.now());

        // 发送到处理队列, 文件已经上传这里设置初始状态发送给处理队列更新状态
        taskInfo.setCurrentState(FileProcessingState.INITIAL);
        taskInfo.setEvent(FileProcessingEvent.UPLOAD_START);
        messageProducer.sendToUploadQueue(taskInfo);
        return ApiBody.success(taskInfo);

    }

    @GetMapping("/files/{id}")
    @Override
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("id") String file) throws Exception {
        Optional<OSSObject> download = objectStorageService.download(file).blockOptional();
        if (download.isEmpty()) {
            throw new Exception("file not found");
        }
        OSSObject ossObject = download.get();
        InputStreamResource resource = new InputStreamResource(ossObject.getObjectContent());

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        // 编码文件名，防止中文乱码
        String encodedFileName = URLEncoder.encode(file, StandardCharsets.UTF_8);
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        // 返回响应实体
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping(value = "/pdfs/converts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ApiBody<String> convertFileToPdf(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("file is empty");
        }

        Flux<DataBuffer> convert = pdfDocumentConverter.convert(file.getInputStream(), file.getOriginalFilename(), file.getSize(), null);
        DataBufferUtils.write(convert, Path.of("E:/1.pdf"),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE).block();
        return ApiBody.success();
    }

    @PostMapping(value = "/files/callback")
    @Override
    public ApiBody<String> callBack(@RequestBody CallbackDTO dto) throws Exception {
        TaskInfo taskInfo = new TaskInfo();
        switch (dto.getStatus()) {
            case 1, 2 -> {
                // TODO: 发送转换消息
            }
            case 3, 4 -> {
                // TODO: 发送切片消息
            }
            default -> {
                throw new Exception("callback status error");
            }
        }
        return ApiBody.success();
    }


}