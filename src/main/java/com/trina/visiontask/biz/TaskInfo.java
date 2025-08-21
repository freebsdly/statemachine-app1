package com.trina.visiontask.biz;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class TaskInfo implements Serializable {
    private UUID id;
    private String taskType;

    private int retryCount;
    private int priority;
    private LocalDateTime processTime;
    private LocalDateTime finishedTime;
    private String message;

    private FileInfo fileInfo;
}
