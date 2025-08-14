package com.trina.visiontask.biz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String fileName;
    private String fileType;
    private String filePath;
    private long fileSize;
}

