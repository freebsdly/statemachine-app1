package com.trina.visiontask.service;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link com.trina.visiontask.repository.entity.FileEntity}
 */

@Data
public class FileDTO implements Serializable {
    private Long id;
    private UUID fileId;
    private String fileName;
    private String fileType;
    private String mimeType;
    private String ossFileKey;
    private String ossPDFKey;
    private String ossMDKey;
    private String filePath;
    private String pdfPath;
    private String mdPath;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    private Long fileSize;
}