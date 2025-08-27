package com.trina.visiontask.service;

import com.trina.visiontask.statemachine.FileProcessingEvent;
import com.trina.visiontask.statemachine.FileProcessingState;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link com.trina.visiontask.repository.entity.TaskEntity}
 */
@Data
public class TaskDTO implements Serializable {
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
    private FileDTO fileInfo;
    private String mdCallbackUrl;
    private String sliceCallbackUrl;
}