package com.trina.visiontask;

import lombok.Getter;

@Getter
public enum FileProcessingState {
    INITIAL(0),               // 初始状态
    UPLOADING(1),             // 上传中
    UPLOADED(2),              // 上传完成
    PDF_CONVERTING(3),     // PDF转换中
    PDF_CONVERTED(4),         // PDF转换完成
    MARKDOWN_CONVERT_SUBMITTING(5), // Markdown转换已提交中
    MARKDOWN_CONVERT_SUBMITTED(6),   // Markdown转换已提交
    MARKDOWN_CONVERTED(7),    // Markdown转换完成
    AI_SLICING(8),         // AI切片处理中
    AI_SLICE_SUBMITTING(9), // AI切片处理已提交中
    AI_SLICE_SUBMITTED(10),   // AI切片处理已提交
    COMPLETED(11),             // 全部完成
    FAILED(12);                // 处理失败

    private final int code;

    FileProcessingState(int code) {
        this.code = code;
    }
}
