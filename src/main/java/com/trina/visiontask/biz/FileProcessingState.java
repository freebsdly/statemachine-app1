package com.trina.visiontask.biz;

public enum FileProcessingState
{
    INITIAL,               // 初始状态
    UPLOADING,             // 上传中
    UPLOADED,              // 上传完成
    PDF_CONVERTING,     // PDF转换中
    PDF_CONVERTED,         // PDF转换完成
    MARKDOWN_CONVERT_SUBMITTING, // Markdown转换已提交中
    MARKDOWN_CONVERT_SUBMITTED,   // Markdown转换已提交
    MARKDOWN_CONVERTED,    // Markdown转换完成
    AI_SLICING,         // AI切片处理中
    AI_SLICE_SUBMITTING, // AI切片处理已提交中
    AI_SLICE_SUBMITTED,   // AI切片处理已提交
    COMPLETED,             // 全部完成
    FAILED                 // 处理失败
}
