package com.trina.visiontask.exception;

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
}

