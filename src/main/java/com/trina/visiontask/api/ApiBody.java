package com.trina.visiontask.api;

import lombok.Data;

@Data
public class ApiBody<T>
{
    private int code;
    private String message;
    private T data;

    public ApiBody(int code, String message, T data)
    {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiBody<T> success()
    {
        return new ApiBody<>(0, "successful", null);
    }

    public static <T> ApiBody<T> success(T data)
    {
        return new ApiBody<>(0, "successful", data);
    }

    public static <T> ApiBody<T> failure(String message)
    {
        return new ApiBody<>(500, message, null);
    }

    public static <T> ApiBody<T> failure(int code, String message)
    {
        return new ApiBody<>(code, message, null);
    }

    public static <T> ApiBody<T> failure(int code, String message, T data)
    {
        return new ApiBody<>(code, message, data);
    }
}
