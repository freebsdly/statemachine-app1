package com.trina.visiontask.statemachine;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class TaskInfo implements Serializable {
    private Long id;
    private UUID taskId;
    private String taskType;
    private int retryCount;
    private int priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;
    private FileProcessingState previousState;
    private FileProcessingState currentState;
    private FileProcessingEvent event;
    private FileInfo fileInfo;
    private String mdCallbackUrl;
    private String sliceCallbackUrl;
}
