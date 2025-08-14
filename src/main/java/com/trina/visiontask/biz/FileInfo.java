package com.trina.visiontask.biz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo implements Serializable
{
    private String fileName;
    private String fileType;
    private String filePath;
    private long fileSize;
}

