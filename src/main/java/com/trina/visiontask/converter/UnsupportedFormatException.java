package com.trina.visiontask.converter;

/**
 * 不支持的文档格式异常
 */
public class UnsupportedFormatException extends ConversionException
{
    public UnsupportedFormatException(String format)
    {
        super("Unsupported format: " + format);
    }

    public UnsupportedFormatException(String format, Throwable cause)
    {
        super("Unsupported format: " + format, cause);
    }

    public UnsupportedFormatException(Exception e)
    {
        super(e);
    }
}
