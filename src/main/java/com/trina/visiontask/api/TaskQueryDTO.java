package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Schema(description = "Task query options")
@Data
public class TaskQueryDTO {
    @Schema(description = "Task Ids")
    private List<UUID> taskIds;
    @Schema(description = "Task States")
    private List<Integer> states;
    @Schema(description = "Task Types")
    private List<String> taskTypes;

    @Schema(description = "Page number")
    private Integer pageNum = 1;
    @Schema(description = "Page size")
    private Integer pageSize = 10;
}
