package com.trina.visiontask.repository.entity;

import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "task_history", indexes = {
        @Index(name = "task_history_task_id_index", columnList = "task_id"),
})
public class TaskHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private UUID taskId;
    private String taskType;
    private int retryCount;
    private int priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Column(length = 2000)
    private String message;
    private FileProcessingState previousState;
    private FileProcessingState currentState;
    private FileProcessingEvent event;
}
