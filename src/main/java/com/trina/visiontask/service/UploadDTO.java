package com.trina.visiontask.service;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadDTO {
    MultipartFile filedata;
    String path;
    String tankId;
    Boolean isConflict;
    String filename;
    String type;
    String operator;
}
