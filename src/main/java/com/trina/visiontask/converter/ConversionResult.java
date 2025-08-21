package com.trina.visiontask.converter;

import lombok.Data;

import java.util.List;

@Data
public class ConversionResult {
    private boolean success;          // 是否成功
    private String outputFormat;      // 输出格式
    private long outputSize;          // 输出大小（字节）
    private String errorMessage;      // 错误信息（失败时非空）
    private List<String> warnings;    // 警告信息（如部分内容转换异常）

    // 构造方法
    public static ConversionResult success(String outputFormat, long outputSize) {
        ConversionResult result = new ConversionResult();
        result.success = true;
        result.outputFormat = outputFormat;
        result.outputSize = outputSize;
        return result;
    }

    public static ConversionResult failure(String errorMessage) {
        ConversionResult result = new ConversionResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

}