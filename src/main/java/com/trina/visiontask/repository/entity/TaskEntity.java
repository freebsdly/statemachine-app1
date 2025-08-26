package com.trina.visiontask.repository.entity;

import com.trina.visiontask.biz.FileProcessingEvent;
import com.trina.visiontask.biz.FileProcessingState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "task_infos", indexes = {
        @Index(name = "task_infos_task_id_index", columnList = "task_id"),
        @Index(name = "task_infos_file_id_index", columnList = "file_id")
})
public class TaskEntity {
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
    private String message;
    private FileProcessingState previousState;
    private FileProcessingState currentState;
    private FileProcessingEvent event;
    @ManyToOne
    @JoinColumn(name = "file_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private FileEntity fileInfo;
}
