package com.trina.visiontask.converter;

import java.util.List;

public record ConversionResult(
        boolean success,          // 是否成功
        String outputFormat,      // 输出格式
        long outputSize,         // 输出大小（字节）
        String errorMessage,      // 错误信息（失败时非空）
        List<String> warnings    // 警告信息（如部分内容转换异常）
) {
    // 构造方法
    public static ConversionResult success(String outputFormat, long outputSize) {
        return new ConversionResult(true, outputFormat, outputSize, null, null);
    }

    public static ConversionResult failure(String errorMessage) {
        return new ConversionResult(false, null, 0, errorMessage, null);
    }

}