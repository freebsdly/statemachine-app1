package com.trina.visiontask.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vision Task API")
public interface ApiDoc {

    @Operation(description = "Callback")
    ApiBody<Void> callBack(@RequestBody CallbackDTO dto) throws Exception;
}