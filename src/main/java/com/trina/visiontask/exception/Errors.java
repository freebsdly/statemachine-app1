package com.trina.visiontask.exception;

import lombok.Getter;

@Getter
public enum Errors {

    K10001("K-10001", "知识库上传文件转换失败"),
    K10002("K-10002", "知识库文件上传失败"),
    S10001("S-10001", "会话保存失败"),
    AI10001("AI-10001", "调用AI服务文件解析失败"),
    AI10002("AI-10002", "AI服务文件解析回调异常"),
    AI10004("AI-10004", "AI服务切片回调异常");

    private final String code;
    private final String desc;

    Errors(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
