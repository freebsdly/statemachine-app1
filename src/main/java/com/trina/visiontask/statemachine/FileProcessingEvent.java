package com.trina.visiontask.statemachine;

public enum FileProcessingEvent
{
    UPLOAD_START,          // 开始上传
    UPLOAD_SUCCESS,        // 上传成功
    UPLOAD_FAILURE,        // 上传失败
    PDF_CONVERT_START,     // 开始PDF转换
    PDF_CONVERT_SUCCESS,   // PDF转换成功
    PDF_CONVERT_FAILURE,   // PDF转换失败
    MD_CONVERT_START,      // 开始Markdown转换
    MD_CONVERT_SUBMIT_SUCCESS, // Markdown转换提交成功
    MD_CONVERT_SUBMIT_FAILURE, // Markdown转换提交失败
    MD_CONVERT_SUCCESS,    // Markdown转换成功
    MD_CONVERT_FAILURE,    // Markdown转换失败
    AI_SLICE_START,      // 开始AI处理
    AI_SLICE_SUBMIT_SUCCESS,   // AI处理提交成功
    AI_SLICE_SUBMIT_FAILURE,   // AI处理提交失败
    AI_SLICE_SUCCESS,    // AI处理成功
    AI_SLICE_FAILURE     // AI处理失败
}
