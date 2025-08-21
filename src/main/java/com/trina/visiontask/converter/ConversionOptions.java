package com.trina.visiontask.converter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 转换参数配置类（按需扩展）
 */
@Data
@NoArgsConstructor
public class ConversionOptions {
    private Map<String, Object> inputOptions;
    private Map<String, Object> outputOptions;
}
