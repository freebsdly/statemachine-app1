package com.trina.visiontask.api;

import java.util.UUID;

public record UploadDocumentResult(UUID id, boolean isConflict) {
}
