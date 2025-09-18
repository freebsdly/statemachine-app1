package com.trina.visiontask.api;

import com.trina.visiontask.service.FileDTO;
import com.trina.visiontask.service.TaskDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File Management API")
public interface FileManagementApiDoc {

    @Operation(description = "Upload File")
    ApiBody<TaskDTO> uploadFile(MultipartFile file, String tankId, boolean force) throws Exception;

    @Operation(description = "Download File")
    ResponseEntity<InputStreamResource> downloadFile(String file) throws Exception;

    @Operation(description = "Get Files")
    ApiBody<Page<FileDTO>> getFiles(@ParameterObject FileQueryDTO options) throws Exception;

    @Operation(description = "Get Tasks")
    ApiBody<Page<TaskDTO>> getTasks(@ParameterObject TaskQueryDTO options) throws Exception;
}
