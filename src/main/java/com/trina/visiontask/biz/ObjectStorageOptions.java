package com.trina.visiontask.biz;

import lombok.Data;

@Data
public class ObjectStorageOptions {
    private String endpoint;
    private String accessKey;
    private String accessSecret;
    private String bucketName;
    private String document;
    private int partSize;
    private int downloadBufferSize;
}
