package com.trina.visiontask.api;

import com.aliyun.oss.model.OSSObject;
import com.trina.visiontask.converter.DocumentConverter;
import com.trina.visiontask.service.ObjectStorageService;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import com.trina.visiontask.statemachine.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class Api implements ApiDoc
{
    private static final Logger log = LoggerFactory.getLogger(Api.class);
    private final FileProcessingService fileProcessingService;
    private final ObjectStorageService objectStorageService;
    private final DocumentConverter pdfDocumentConverter;
    private final TaskService taskService;
    private final ApiMapper apiMapper;

    public Api(
            FileProcessingService fileProcessingService,
            ObjectStorageService objectStorageService,
            @Qualifier("PDFDocumentConverter") DocumentConverter pdfDocumentConverter,
            TaskService taskService,
            ApiMapper apiMapper)
    {
        this.fileProcessingService = fileProcessingService;
        this.objectStorageService = objectStorageService;
        this.pdfDocumentConverter = pdfDocumentConverter;
        this.taskService = taskService;
        this.apiMapper = apiMapper;
    }

    @Override
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiBody<TaskDTO> uploadFile(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "force") boolean force) throws Exception
    {
        TaskDTO taskInfo = taskService.uploadFile(file, force);
        return ApiBody.success(taskInfo);

    }

    @GetMapping("/files/download/{id}")
    @Override
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("id") String file) throws Exception
    {
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
                .contentLength(ossObject.getObjectMetadata().getContentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping(value = "/files/converts/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ApiBody<String> convertFileToPdf(MultipartFile file) throws Exception
    {
        if (file.isEmpty()) {
            throw new Exception("file is empty");
        }

        Flux<DataBuffer> convert = pdfDocumentConverter.convert(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize(),
                null);
        DataBufferUtils.write(convert, Path.of("E:/1.pdf"),
                              StandardOpenOption.CREATE,
                              StandardOpenOption.TRUNCATE_EXISTING,
                              StandardOpenOption.WRITE).block();
        return ApiBody.success();
    }

    @PostMapping(value = "/files/converts/callback")
    @Override
    public ApiBody<Void> callBack(@RequestBody CallbackDTO dto) throws Exception
    {
        fileProcessingService.processCallback(apiMapper.to(dto));
        return ApiBody.success();
    }
}