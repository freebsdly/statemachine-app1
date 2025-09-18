package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "兼容接口返回结果")
public record CompatibleApiBody<T>(int code, String msg, T data) {

    public static <T> CompatibleApiBody<T> success() {
        return new CompatibleApiBody<>(200, "操作成功", null);
    }

    public static <T> CompatibleApiBody<T> success(T data) {
        return new CompatibleApiBody<>(200, "操作成功", data);
    }

    public static <T> CompatibleApiBody<T> failure(String message) {
        return new CompatibleApiBody<>(500, message, null);
    }

    public static <T> CompatibleApiBody<T> failure(int code, String message) {
        return new CompatibleApiBody<>(code, message, null);
    }

    public static <T> CompatibleApiBody<T> failure(int code, String message, T data) {
        return new CompatibleApiBody<>(code, message, data);
    }
}
