package com.trina.visiontask.biz;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadProducer
{

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${upload.consumer.queue.name}")
    private String queueName;

    @Value("${upload.consumer.queue.x-max-priority}")
    private String queueXMaxPriority;

    @Value("${upload.consumer.exchange.name}")
    private String exchangeName;

    @Value("${upload.consumer.routing.key}")
    private String routingKey;

    public void sendFileInfo(FileInfo fileInfo) throws AmqpException
    {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, fileInfo, message -> {
            // 设置优先级属性
            message.getMessageProperties().setPriority(5);
            return message;
        });
    }


}
