package com.trina.visiontask.api;

import com.trina.visiontask.statemachine.CallbackInfo;
import com.trina.visiontask.statemachine.FileProcessingService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Api implements ApiDoc {
    private static final Logger log = LoggerFactory.getLogger(Api.class);
    private final FileProcessingService fileProcessingService;
    private final ApiMapper apiMapper;

    public Api(
            FileProcessingService fileProcessingService,
            ApiMapper apiMapper) {
        this.fileProcessingService = fileProcessingService;
        this.apiMapper = apiMapper;
    }

    @Timed(
            value = "api.callback",
            description = "md convert and ai slice callback",
            percentiles = {0.5, 0.95},
            histogram = true
    )
    @PostMapping(value = "/files/converts/callback")
    @Override
    public ApiBody<Void> callBack(@RequestBody CallbackDTO dto) throws Exception {
        log.debug("ai service callback: {}", dto);
        CallbackInfo callbackInfo = apiMapper.to(dto);
        fileProcessingService.processCallback(callbackInfo);
        return ApiBody.success();
    }
}