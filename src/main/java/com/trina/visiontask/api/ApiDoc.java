package com.trina.visiontask.api;

import com.trina.visiontask.biz.TaskInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Vision Task API")
public interface ApiDoc {

    @Operation(description = "File Processing")
    public String processFile(TaskInfo info) throws Exception;

    @Operation(description = "Operate Consumer")
    public String operateConsumer(String operate, String consumerId) throws Exception;

    @Operation(description = "Upload File")
    public TaskInfo uploadFile(MultipartFile file) throws Exception;
}