package com.trina.visiontask;

import com.trina.visiontask.biz.FileProcessingEvent;
import com.trina.visiontask.biz.FileProcessingState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vision Task API")
public interface ApiDoc {

    @Operation(description = "File Processing")
    public String processFile(FileProcessingState state, FileProcessingEvent event);
}
