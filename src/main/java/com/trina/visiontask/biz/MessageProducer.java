package com.trina.visiontask.biz;

import com.trina.visiontask.MQConfiguration;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer
{

    private final RabbitTemplate rabbitTemplate;
    private final MQConfiguration uploadConfig;
    private final MQConfiguration pdfConvertConfig;
    private final MQConfiguration mdConvertConfig;
    private final MQConfiguration aiSliceConfig;

    public MessageProducer(RabbitTemplate rabbitTemplate,
                           @Qualifier("uploadConfiguration") MQConfiguration uploadConfig,
                           @Qualifier("pdfConvertConfiguration") MQConfiguration pdfConvertConfig,
                           @Qualifier("mdConvertConfiguration") MQConfiguration mdConvertConfig,
                           @Qualifier("aiSliceConfiguration") MQConfiguration aiSliceConfig)
    {
        this.rabbitTemplate = rabbitTemplate;
        this.uploadConfig = uploadConfig;
        this.pdfConvertConfig = pdfConvertConfig;
        this.mdConvertConfig = mdConvertConfig;
        this.aiSliceConfig = aiSliceConfig;
    }


    public void sendToUploadQueue(TaskInfo info) throws AmqpException
    {
        sendMessage(uploadConfig, info);
    }

    public void sendToPdfConvertQueue(TaskInfo info) throws AmqpException
    {
        sendMessage(pdfConvertConfig, info);
    }

    public void sendToMdConvertQueue(TaskInfo info) throws AmqpException
    {
        sendMessage(mdConvertConfig, info);
    }

    public void sendToAiSliceQueue(TaskInfo info) throws AmqpException
    {
        sendMessage(aiSliceConfig, info);
    }

    private void sendMessage(MQConfiguration config, TaskInfo info) throws AmqpException
    {
        rabbitTemplate.convertAndSend(config.getExchangeName(), config.getRoutingKey(), info, message -> {
            // 设置优先级属性
            message.getMessageProperties().setPriority(info.getPriority());
            return message;
        });
    }
}