package com.trina.visiontask.converter;

/**
 * 转换过程中发生的通用异常
 */
public class ConversionException extends Exception {
    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionException(Exception e) {
        super(e);
    }
}

