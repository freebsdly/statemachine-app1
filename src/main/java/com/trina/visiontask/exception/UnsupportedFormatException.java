package com.trina.visiontask.exception;

/**
 * 不支持的文档格式异常
 */
public class UnsupportedFormatException extends ConversionException {
    public UnsupportedFormatException(String format) {
        super("Unsupported format: " + format);
    }
}
