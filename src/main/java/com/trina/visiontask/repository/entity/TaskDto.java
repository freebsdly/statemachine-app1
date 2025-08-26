package com.trina.visiontask.repository.entity;

import com.trina.visiontask.biz.FileProcessingEvent;
import com.trina.visiontask.biz.FileProcessingState;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link TaskEntity}
 */
@Data
public class TaskDto implements Serializable {
    Long id;
    UUID taskId;
    String taskType;
    int retryCount;
    int priority;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String message;
    FileProcessingState previousState;
    FileProcessingState currentState;
    FileProcessingEvent event;
    FileDto fileInfo;
}