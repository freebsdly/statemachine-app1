package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vision Compatible API")
public interface VisionCompatibleApiDoc {

    @Operation(summary = "检查文件解析状态", description = "Check File Status")
    ApiBody<String> checkFileStatus(
            @Parameter(description = "File Id", required = true, in = ParameterIn.QUERY, name = "docId") String id
    ) throws Exception;

    @Operation(summary = "解析知识库文件", description = "Parse File")
    ApiBody<String> parseFile(
            @Parameter(description = "File Id", required = true, in = ParameterIn.QUERY, name = "ossId") String id
    ) throws Exception;

    @Operation(summary = "上传知识库文件", description = "Upload Document")
    ApiBody<String> uploadDocument(
            @Parameter(description = "File Id", required = true, in = ParameterIn.QUERY, name = "ossId") String id
    ) throws Exception;

    @Operation(summary = "MD转换和AI切片回调接口", description = "Save File Status")
    ApiBody<String> saveFileStatus(@RequestBody CallbackDTO dto) throws Exception;
}
