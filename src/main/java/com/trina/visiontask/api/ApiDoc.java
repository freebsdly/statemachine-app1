package com.trina.visiontask.api;

import com.trina.visiontask.biz.TaskInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Vision Task API")
public interface ApiDoc
{

    @Operation(description = "File Processing")
    public ApiBody<String> processFile(TaskInfo info) throws Exception;

    @Operation(description = "Upload File")
    public ApiBody<TaskInfo> uploadFile(MultipartFile file) throws Exception;

    @Operation(description = "Download File")
    public ResponseEntity<InputStreamResource> downloadFile(String file) throws Exception;

    @Operation(description = "Convert File to PDF")
    public ApiBody<String> convertFileToPdf(MultipartFile file) throws Exception;
}