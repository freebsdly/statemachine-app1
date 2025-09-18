package com.trina.visiontask.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
public class ReparseDTO {
    private UUID fileId;
    private String operator;

    public ReparseDTO(UUID fileId, String operator) {
        this.fileId = fileId;
        this.operator = operator;
    }
}
