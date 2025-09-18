package com.trina.visiontask.converter;

import java.util.UUID;


public record AlgRequestDTO(
        UUID itemId,
        String key,
        Long timestamp,
        String envId,
        String tags
) {
}
