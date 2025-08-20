package com.trina.visiontask;

import com.trina.visiontask.biz.FileInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vision Task API")
public interface ApiDoc {

    @Operation(description = "File Processing")
    public String processFile(FileInfo info) throws Exception;

    @Operation(description = "Operate Consumer")
    public String operateConsumer(String operate, String consumerId) throws Exception;
}
