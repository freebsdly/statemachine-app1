package com.trina.visiontask.biz;

import com.trina.visiontask.Config;
import lombok.AllArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor_ = @Autowired)
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    private final Config config;

    public void sendToUploadQueue(FileInfo fileInfo) throws AmqpException {
        rabbitTemplate.convertAndSend(config.uploadExchangeName, config.uploadRoutingKey, fileInfo, message -> {
            // 设置优先级属性
            int priority = calculatePriority(fileInfo);
            message.getMessageProperties().setPriority(priority);
            return message;
        });
    }

    public void sendToPdfConvertQueue(FileInfo fileInfo) throws AmqpException {
        rabbitTemplate.convertAndSend(config.pdfConvertExchangeName, config.pdfConvertRoutingKey, fileInfo, message -> {
            // 设置优先级属性
            int priority = calculatePriority(fileInfo);
            message.getMessageProperties().setPriority(priority);
            return message;
        });
    }

    public void sendToMdConvertQueue(FileInfo fileInfo) throws AmqpException {
        rabbitTemplate.convertAndSend(config.mdConvertExchangeName, config.mdConvertRoutingKey, fileInfo, message -> {
            // 设置优先级属性
            int priority = calculatePriority(fileInfo);
            message.getMessageProperties().setPriority(priority);
            return message;
        });
    }

    public void sendToAiSliceQueue(FileInfo fileInfo) throws AmqpException {
        rabbitTemplate.convertAndSend(config.aiSliceExchangeName, config.aiSliceRoutingKey, fileInfo, message -> {
            // 设置优先级属性
            int priority = calculatePriority(fileInfo);
            message.getMessageProperties().setPriority(priority);
            return message;
        });
    }

    public int calculatePriority(FileInfo fileInfo) {
        // Calculate priority based on file size
        return 5;
    }


}
