package com.trina.visiontask.api;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.trina.visiontask.biz.FileInfo;
import com.trina.visiontask.biz.MessageProducer;
import com.trina.visiontask.biz.ObjectStorageService;
import com.trina.visiontask.biz.TaskInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
public class Api implements ApiDoc {

    private final MessageProducer messageProducer;
    private final ObjectStorageService objectStorageService;
    private final RabbitListenerEndpointRegistry registry;

    public Api(MessageProducer messageProducer,
               ObjectStorageService objectStorageService,
               RabbitListenerEndpointRegistry registry) {
        this.messageProducer = messageProducer;
        this.objectStorageService = objectStorageService;
        this.registry = registry;
    }


    @Override
    @PostMapping("/process-file")
    public String processFile(@RequestBody TaskInfo info) throws Exception {
        messageProducer.sendToUploadQueue(info);
        return "文件处理流程已启动";
    }

    @PostMapping("/consumers")
    @Override
    public String operateConsumer(@RequestParam("operate") String operate, @RequestParam("id") String consumerId) throws Exception {
        MessageListenerContainer container = getContainer(consumerId);
        if (container.isRunning()) {
            container.stop();
            return "消费者 " + consumerId + " 停止成功";
        } else {
            container.start();
            return "消费者 " + consumerId + " 启动成功";
        }
    }

    @Override
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaskInfo uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        // 构造TaskInfo对象
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setId(UUID.randomUUID());
        if (file.isEmpty()) {
            taskInfo.setMessage("文件为空");
            return taskInfo;
        }
        String suffix = null;
        if (file.getOriginalFilename() != null && file.getOriginalFilename().lastIndexOf(".") != -1) {
            suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        }
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(UUID.randomUUID());
        fileInfo.setFileName(file.getOriginalFilename());
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setFileType(suffix);
        Mono<CompleteMultipartUploadResult> upload = objectStorageService.upload(fileInfo.getId().toString(), file.getInputStream());
        CompleteMultipartUploadResult result = upload.block();
        fileInfo.setOssFileKey(result.getKey());
        fileInfo.setFilePath(result.getLocation());
        fileInfo.setFileSize(file.getSize());
        taskInfo.setFileInfo(fileInfo);

        // 发送到处理队列
        messageProducer.sendToUploadQueue(taskInfo);
        return taskInfo;

    }

    private MessageListenerContainer getContainer(String consumerId) {
        return registry.getListenerContainer(consumerId);
    }
}