package com.trina.visiontask.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "回调参数")
public record CallbackDTO(
        @Schema(description = "文件的唯一ID")
        @JsonProperty("itemId")
        UUID taskId,

        @Schema(description = "文件路径")
        String key,

        @Schema(description = "文件名称")
        @JsonProperty("name")
        String fileName,

        @Schema(description = "1、解析成功,2、解析失败,3、切片成功,4、切片失败")
        Integer status,

        @Schema(description = "消息说明")
        String message
) {
}
