package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "OSS对象")
public class OssObjectDTO {

    @Schema(description = "文件名称")
    private String objectName;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "文件更新时间")
    private String updateTime;

    @Schema(description = "唯一标识")
    private String ossId;

    @Schema(description = "是否是文件夹")
    private Boolean isDir;

    @Schema(description = "文件状态")
    private Integer status;

    @Schema(description = "会话Id")
    private String sessionId;

    @Schema(description = "会话名称")
    private String sessionName;

    @Schema(description = "原始文件Id")
    private String originalId;

    @Schema(description = "原始文件名称")
    private String originalName;

    @Schema(description = "原始文件pdf转化状态0-初始化 1-成功 2-失败")
    private Integer pdfStatus;

    @Schema(description = "转换时间")
    private LocalDateTime convertTime;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "文件名称")
    private String showFileName;
}
