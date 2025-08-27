package com.trina.visiontask.converter;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AlgRequestDTO implements Serializable {
    private String itemId;
    private String key;
    private Long timestamp;
    private String envId;
}
