package com.trina.visiontask;

import lombok.Getter;

@Getter
public enum FileProcessingEvent {
    UPLOAD_START(0),          // 开始上传
    UPLOAD_SUCCESS(1),        // 上传成功
    UPLOAD_FAILURE(2),        // 上传失败
    PDF_CONVERT_START(3),     // 开始PDF转换
    PDF_CONVERT_SUCCESS(4),   // PDF转换成功
    PDF_CONVERT_FAILURE(5),   // PDF转换失败
    MD_CONVERT_START(6),      // 开始Markdown转换
    MD_CONVERT_SUBMIT_SUCCESS(7), // Markdown转换提交成功
    MD_CONVERT_SUBMIT_FAILURE(8), // Markdown转换提交失败
    MD_CONVERT_SUCCESS(9),    // Markdown转换成功
    MD_CONVERT_FAILURE(10),    // Markdown转换失败
    AI_SLICE_START(11),      // 开始AI处理
    AI_SLICE_SUBMIT_SUCCESS(12),   // AI处理提交成功
    AI_SLICE_SUBMIT_FAILURE(13),   // AI处理提交失败
    AI_SLICE_SUCCESS(14),    // AI处理成功
    AI_SLICE_FAILURE(15);     // AI处理失败

    private final int code;

    FileProcessingEvent(int code) {
        this.code = code;
    }
}
