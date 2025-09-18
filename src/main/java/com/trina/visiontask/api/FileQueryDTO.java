package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Schema(description = "File query options")
@Data
public class FileQueryDTO {
    @Schema(description = "File name")
    List<String> fileNames;
    @Schema(description = "File id")
    List<UUID> fileIds;
    @Schema(description = "File status")
    List<Integer> status;
    @Schema(description = "File type")
    List<String> fileTypes;
    @Schema(description = "File tank id")
    List<String> paths;
    @Schema(description = "Page number")
    int pageNum = 1;
    @Schema(description = "Page size")
    int pageSize = 10;
}
