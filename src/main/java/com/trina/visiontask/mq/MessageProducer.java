package com.trina.visiontask.mq;

import com.trina.visiontask.MQConfiguration;
import com.trina.visiontask.service.TaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {
    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final RabbitTemplate rabbitTemplate;
    private final MQConfiguration uploadConfig;
    private final MQConfiguration pdfConvertConfig;
    private final MQConfiguration mdConvertConfig;
    private final MQConfiguration aiSliceConfig;
    private final MQConfiguration taskLogConfig;

    public MessageProducer(RabbitTemplate rabbitTemplate,
                           @Qualifier("uploadConfiguration") MQConfiguration uploadConfig,
                           @Qualifier("pdfConvertConfiguration") MQConfiguration pdfConvertConfig,
                           @Qualifier("mdConvertConfiguration") MQConfiguration mdConvertConfig,
                           @Qualifier("aiSliceConfiguration") MQConfiguration aiSliceConfig,
                           @Qualifier("taskLogConfiguration") MQConfiguration taskLogConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.uploadConfig = uploadConfig;
        this.pdfConvertConfig = pdfConvertConfig;
        this.mdConvertConfig = mdConvertConfig;
        this.aiSliceConfig = aiSliceConfig;
        this.taskLogConfig = taskLogConfig;
    }


    public void sendToUploadQueue(TaskDTO info) throws AmqpException {
        sendMessage(uploadConfig, info);
    }

    public void sendToPdfConvertQueue(TaskDTO info) throws AmqpException {
        sendMessage(pdfConvertConfig, info);
    }

    public void sendToMdConvertQueue(TaskDTO info) throws AmqpException {
        sendMessage(mdConvertConfig, info);
    }

    public void sendToAiSliceQueue(TaskDTO info) throws AmqpException {
        sendMessage(aiSliceConfig, info);
    }

    public void sendToTaskLogQueue(TaskDTO info) throws AmqpException {
        sendMessage(taskLogConfig, info);
    }

    private void sendMessage(MQConfiguration config, TaskDTO info) throws AmqpException {
        rabbitTemplate.convertAndSend(config.getExchangeName(), config.getRoutingKey(), info, message -> {
            // 设置优先级属性
            message.getMessageProperties().setPriority(info.getPriority());
            return message;
        });
    }
}