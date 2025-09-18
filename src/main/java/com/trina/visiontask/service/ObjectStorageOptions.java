package com.trina.visiontask.service;

import lombok.Data;

@Data
public class ObjectStorageOptions {
    private String endpoint;
    private String accessKey;
    private String accessSecret;
    private String bucketName;
    private String prefix;
    private int partSize;
    private int downloadBufferSize;
}
