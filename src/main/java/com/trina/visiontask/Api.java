package com.trina.visiontask;

import com.trina.visiontask.biz.FileInfo;
import com.trina.visiontask.biz.MessageProducer;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor(onConstructor_ = @Autowired)
public class Api implements ApiDoc {

    private final MessageProducer messageProducer;

    private final RabbitListenerEndpointRegistry registry;

    @Override
    @PostMapping("/process-file")
    public String processFile(@RequestBody FileInfo info) throws Exception {
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

    private MessageListenerContainer getContainer(String consumerId) {
        return registry.getListenerContainer(consumerId);
    }
}
