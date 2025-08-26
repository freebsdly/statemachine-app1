package com.trina.visiontask.biz;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FileInfo implements Serializable {
    private Long id;
    private UUID FileId;
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
    private long fileSize;
}

