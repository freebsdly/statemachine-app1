package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Tag(name = "Vision Compatible API")
public interface VisionCompatibleApiDoc {

    @Operation(summary = "解析知识库文件", description = "Parse File")
    CompatibleApiBody<Boolean> parseFile(
            @Parameter(description = "File Id", required = true, in = ParameterIn.QUERY, name = "ossId") UUID fileId
    ) throws Exception;

    @Operation(summary = "上传知识库文件", description = "Upload Document")
    CompatibleApiBody<UploadDocumentResult> uploadDocument(UploadDocumentDTO uploadDocumentDTO) throws Exception;

    @Operation(summary = "MD转换和AI切片回调接口", description = "Save File Status")
    CompatibleApiBody<String> saveFileStatus(CallbackDTO dto) throws Exception;
}
