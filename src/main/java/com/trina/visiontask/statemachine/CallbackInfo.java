package com.trina.visiontask.statemachine;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class CallbackInfo {
    @JsonProperty("itemId")
    private UUID taskId;
    private String key;
    @JsonProperty("name")
    private String fileName;
    private Integer status;
    private String message;
}
