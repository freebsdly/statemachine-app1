package com.trina.visiontask.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "回调参数")
public class CallbackDTO {

    @Schema(description = "文件的唯一ID")
    @JsonProperty("itemId")
    private String taskId;

    @Schema(description = "文件路径")
    private String key;

    @Schema(description = "文件名称")
    @JsonProperty("name")
    private String fileName;

    @Schema(description = "1、解析成功,2、解析失败,3、切片成功,4、切片失败")
    private Integer status;

    @Schema(description = "消息说明")
    private String message;
}
