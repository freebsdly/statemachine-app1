package com.trina.visiontask;

import lombok.Data;

@Data
public class TaskConfiguration {
    /**
     * 任务信息key
     */
    private String taskInfoKey;

    /**
     * 失败任务最大重试次数
     */
    private int maxRetryCount;

    /**
     * 异步回调等待任务超时时间
     */
    private long waitTimeout;
    /**
     * 上传任务超时
     */
    private long uploadTaskTimeout;
    /**
     * PDF转换任务超时
     */
    private long pdfConvertTaskTimeout;
    /**
     * Markdown转换任务超时
     */
    private long mdConvertTaskTimeout;
    /**
     * AI处理任务超时
     */
    private long aiSliceTaskTimeout;
}
