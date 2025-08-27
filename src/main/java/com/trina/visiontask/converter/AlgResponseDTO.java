package com.trina.visiontask.converter;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AlgResponseDTO implements Serializable {
    private boolean success;
    private String errMsg;
    private String errCode;
}
