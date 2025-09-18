package com.trina.visiontask.service;

import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
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
    private String operator;

    // 检查源文件类型
    // TODO: 去除硬编码
    public boolean checkSupportedFileType() {
        if (fileInfo == null) {
            return false;
        }
        String fileType = fileInfo.getFileType();
        if (fileType == null) {
            return false;
        }
        String check = fileType.toLowerCase();
        switch (taskType) {
            case "upload" -> {
                return true;
            }
            case "pdf-convert" -> {
                return check.equals("doc")
                        || check.equals("docx")
                        || check.equals("ppt")
                        || check.equals("pptx");
            }
            case "md-convert", "ai-slice" -> {
                return check.equals("pdf")
                        || check.equals("doc")
                        || check.equals("docx")
                        || check.equals("ppt")
                        || check.equals("pptx");
            }
            default -> {
                return false;
            }
        }
    }
}