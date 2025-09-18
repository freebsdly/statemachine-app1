package com.trina.visiontask.converter;

public record AlgResponseDTO(
        boolean success,
        String errMsg,
        String errCode
) {
}
