package com.trina.visiontask.converter;

import java.util.Map;

/**
 * 转换参数配置类（按需扩展）
 */
public record ConversionOptions(
        Map<String, Object> inputOptions,
        Map<String, Object> outputOptions
) {
}
