package com.trina.visiontask.api;

import com.aliyun.oss.model.OSSObject;
import com.trina.visiontask.service.FileDTO;
import com.trina.visiontask.service.ObjectStorageService;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileManagementApi implements FileManagementApiDoc {
    private static final Logger log = LoggerFactory.getLogger(Api.class);
    private final ObjectStorageService objectStorageService;
    private final TaskService taskService;
    private final ApiMapper apiMapper;

    public FileManagementApi(
            ObjectStorageService objectStorageService,
            TaskService taskService,
            ApiMapper apiMapper) {
        this.objectStorageService = objectStorageService;
        this.taskService = taskService;
        this.apiMapper = apiMapper;
    }

    @Timed(
            value = "api.uploadFile",
            description = "upload file",
            percentiles = {0.5, 0.95},
            histogram = true
    )
    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiBody<TaskDTO> uploadFile(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "0") String tankId,
                                       @RequestParam(value = "force") boolean force) throws Exception {
        throw new Exception("not implemented");
    }

    @GetMapping("/{id}")
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
                .contentLength(ossObject.getObjectMetadata().getContentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping
    @Override
    public ApiBody<Page<FileDTO>> getFiles(@ModelAttribute FileQueryDTO options) throws Exception {
        Page<FileDTO> files = taskService.getFiles(apiMapper.to(options));
        return ApiBody.success(files);
    }

    @GetMapping("/tasks")
    @Override
    public ApiBody<Page<TaskDTO>> getTasks(TaskQueryDTO options) throws Exception {
        Page<TaskDTO> tasks = taskService.getTasks(apiMapper.to(options));
        return ApiBody.success(tasks);
    }
}
