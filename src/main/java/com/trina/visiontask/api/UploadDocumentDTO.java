package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "上传文件参数")
public class UploadDocumentDTO {
    @Schema(description = "文件", requiredMode = Schema.RequiredMode.REQUIRED)
    MultipartFile filedata;
    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String path;
    @Schema(description = "文件存储桶", requiredMode = Schema.RequiredMode.REQUIRED)
    String tankId;
    @Schema(description = "是否冲突", requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean isConflict;
    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String filename;
    @Schema(description = "文件类型")
    String type;
}
