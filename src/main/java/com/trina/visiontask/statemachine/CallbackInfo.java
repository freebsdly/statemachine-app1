package com.trina.visiontask.statemachine;

import lombok.Data;

@Data
public class CallbackInfo {
    private String taskId;
    private String key;
    private String fileName;
    private Integer status;
    private String message;
}
