package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API返回结果")
public record ApiBody<T>(int code, String message, T data) {
    public static <T> ApiBody<T> success() {
        return new ApiBody<>(0, "successful", null);
    }

    public static <T> ApiBody<T> success(T data) {
        return new ApiBody<>(0, "successful", data);
    }

    public static <T> ApiBody<T> failure(String message) {
        return new ApiBody<>(500, message, null);
    }

    public static <T> ApiBody<T> failure(int code, String message) {
        return new ApiBody<>(code, message, null);
    }
}
