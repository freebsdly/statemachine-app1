package com.trina.visiontask.biz;

public enum FileProcessingState {
    INITIAL,               // 初始状态
    UPLOADING,             // 上传中
    UPLOADED,              // 上传完成
    CONVERTING_TO_PDF,     // PDF转换中
    PDF_CONVERTED,         // PDF转换完成
    CONVERTING_TO_MARKDOWN,// Markdown转换中
    MARKDOWN_CONVERTED,    // Markdown转换完成
    AI_PROCESSING,         // AI切片处理中
    COMPLETED,             // 全部完成
    FAILED                 // 处理失败
}
