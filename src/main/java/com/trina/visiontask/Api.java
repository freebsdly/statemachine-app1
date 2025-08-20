package com.trina.visiontask;

import com.trina.visiontask.biz.FileInfo;
import com.trina.visiontask.biz.MessageProducer;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor(onConstructor_ = @Autowired)
public class Api implements ApiDoc {

    private final MessageProducer messageProducer;

    @Override
    @PostMapping("/process-file")
    public String processFile(@RequestBody FileInfo info) throws Exception {
        messageProducer.sendToUploadQueue(info);
        return "文件处理流程已启动";
    }
}
