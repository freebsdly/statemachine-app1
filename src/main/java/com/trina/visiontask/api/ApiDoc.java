package com.trina.visiontask.api;

import com.trina.visiontask.service.TaskDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Vision Task API")
public interface ApiDoc {

    @Operation(description = "Upload File")
    ApiBody<TaskDTO> uploadFile(MultipartFile file) throws Exception;

    @Operation(description = "Download File")
    ResponseEntity<InputStreamResource> downloadFile(String file) throws Exception;

    @Operation(description = "Convert File to PDF")
    ApiBody<String> convertFileToPdf(MultipartFile file) throws Exception;

    @Operation(description = "Callback")
    ApiBody<String> callBack(@RequestBody CallbackDTO dto) throws Exception;
}