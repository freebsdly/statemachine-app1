package com.trina.visiontask.converter;

import lombok.Data;

import java.util.Map;

@Data
public class ConverterOptions {
    private String url;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
    Map<String, String> headers;
    private String envId;
}
