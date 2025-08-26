package com.trina.visiontask.repository.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link FileEntity}
 */
@Data
public class FileDto implements Serializable {
    Long id;
    UUID fileId;
    String fileName;
    String fileType;
    String mimeType;
    String ossFileKey;
    String ossPDFKey;
    String ossMDKey;
    String filePath;
    String pdfPath;
    String mdPath;
    LocalDateTime createTime;
    LocalDateTime modifyTime;
    Long fileSize;
}