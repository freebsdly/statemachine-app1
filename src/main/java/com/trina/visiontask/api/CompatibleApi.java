package com.trina.visiontask.api;

import com.trina.visiontask.statemachine.FileProcessingService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document")
public class CompatibleApi implements VisionCompatibleApiDoc {
    private static final Logger log = LoggerFactory.getLogger(CompatibleApi.class);

    private final FileProcessingService fileProcessingService;
    private final ApiMapper apiMapper;

    public CompatibleApi(FileProcessingService fileProcessingService, ApiMapper apiMapper) {
        this.fileProcessingService = fileProcessingService;
        this.apiMapper = apiMapper;
    }

    @Timed(value = "api.checkFileStatus", description = "check file status")
    @GetMapping(value = "/checkStatus")
    @Override
    public ApiBody<String> checkFileStatus(String id) throws Exception {
        throw new Exception("not implemented");
    }

    @Timed(value = "api.parseFile", description = "parse file")
    @GetMapping("/fileParse")
    @Override
    public ApiBody<String> parseFile(String id) throws Exception {
        throw new Exception("not implemented");
    }

    @Timed(value = "api.uploadDocument", description = "upload document")
    @PostMapping(value = "/uploadDocument")
    @Override
    public ApiBody<String> uploadDocument(String id) throws Exception {
        throw new Exception("not implemented");
    }

    @Timed(value = "api.saveFileStatus", description = "save file status")
    @PostMapping(value = "/saveFileStatus")
    @Override
    public ApiBody<String> saveFileStatus(CallbackDTO dto) throws Exception {
        fileProcessingService.processCallback(apiMapper.to(dto));
        return ApiBody.success();
    }
}
