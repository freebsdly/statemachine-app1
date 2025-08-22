package com.trina.visiontask.api;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.trina.visiontask.biz.FileInfo;
import com.trina.visiontask.biz.MessageProducer;
import com.trina.visiontask.biz.ObjectStorageService;
import com.trina.visiontask.biz.TaskInfo;
import com.trina.visiontask.converter.DocumentConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class Api implements ApiDoc {

    private final MessageProducer messageProducer;
    private final ObjectStorageService objectStorageService;
    private final DocumentConverter pdfDocumentConverter;
    private final DataBufferFactory dataBufferFactory;

    public Api(MessageProducer messageProducer,
               ObjectStorageService objectStorageService,
               @Qualifier("PDFDocumentConverter") DocumentConverter pdfDocumentConverter,
               @Qualifier("dataBufferFactory") DataBufferFactory dataBufferFactory
    ) {
        this.messageProducer = messageProducer;
        this.objectStorageService = objectStorageService;
        this.pdfDocumentConverter = pdfDocumentConverter;
        this.dataBufferFactory = dataBufferFactory;
    }


    @Override
    @PostMapping("/process-file")
    public String processFile(@RequestBody TaskInfo info) throws Exception {
        messageProducer.sendToUploadQueue(info);
        return "文件处理流程已启动";
    }

    @Override
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaskInfo uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        // 构造TaskInfo对象
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setId(UUID.randomUUID());
        if (file.isEmpty()) {
            taskInfo.setMessage("文件为空");
            return taskInfo;
        }
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

        // 发送到处理队列
        messageProducer.sendToPdfConvertQueue(taskInfo);
        return taskInfo;

    }

    @GetMapping("/download-file")
    @Override
    public ResponseEntity<InputStreamResource> downloadFile(String file) throws Exception {
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
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        // 返回响应实体
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping(value = "/converts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public String convertFileToPdf(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("file is empty");
        }

        Flux<DataBuffer> convert = pdfDocumentConverter.convert(file, null);
        DataBufferUtils.write(convert, Path.of("E:/1.pdf"),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE).block();
        return "ok";
    }
}