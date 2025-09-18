package com.trina.visiontask.service;

import com.trina.visiontask.MQConfiguration;
import com.trina.visiontask.TaskConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {
    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final KafkaTemplate<String, TaskDTO> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final MQConfiguration uploadConfig;
    private final MQConfiguration pdfConvertConfig;
    private final MQConfiguration mdConvertConfig;
    private final MQConfiguration aiSliceConfig;
    private final MQConfiguration taskLogConfig;
    private final TaskConfiguration taskConfiguration;

    public MessageProducer(
            KafkaTemplate<String, TaskDTO> kafkaTemplate,
            RabbitTemplate rabbitTemplate,
            @Qualifier("uploadConfiguration") MQConfiguration uploadConfig,
            @Qualifier("pdfConvertConfiguration") MQConfiguration pdfConvertConfig,
            @Qualifier("mdConvertConfiguration") MQConfiguration mdConvertConfig,
            @Qualifier("aiSliceConfiguration") MQConfiguration aiSliceConfig,
            @Qualifier("taskLogConfiguration") MQConfiguration taskLogConfig,
            TaskConfiguration taskConfiguration
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.uploadConfig = uploadConfig;
        this.pdfConvertConfig = pdfConvertConfig;
        this.mdConvertConfig = mdConvertConfig;
        this.aiSliceConfig = aiSliceConfig;
        this.taskLogConfig = taskLogConfig;
        this.taskConfiguration = taskConfiguration;
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

    public void sendToTaskLogKafka(TaskDTO info) {
        kafkaTemplate.send(taskConfiguration.getKafkaTopic(), info.getTaskId().toString(), info);
    }

    /**
     * 根据taskId计算分片索引
     * 使用一致性哈希确保相同taskId总是路由到同一分片
     */
    private int getShardIndex(String taskId) {
        return Math.abs(taskId.hashCode()) % taskLogConfig.getQueueCount();
    }
}