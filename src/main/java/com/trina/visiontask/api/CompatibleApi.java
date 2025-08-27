package com.trina.visiontask.api;

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

    @GetMapping(value = "/checkStatus")
    @Override
    public ApiBody<String> checkFileStatus(String id) throws Exception {
        throw new Exception("not implemented");
    }

    @GetMapping("/fileParse")
    @Override
    public ApiBody<String> parseFile(String id) throws Exception {
        throw new Exception("not implemented");
    }

    @PostMapping(value = "/uploadDocument")
    @Override
    public ApiBody<String> uploadDocument(String id) throws Exception {
        throw new Exception("not implemented");
    }

    @PostMapping(value = "/saveFileStatus")
    @Override
    public ApiBody<String> saveFileStatus(CallbackDTO dto) throws Exception {
        throw new Exception("not implemented");
    }
}
