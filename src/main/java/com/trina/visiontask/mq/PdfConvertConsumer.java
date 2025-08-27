package com.trina.visiontask.mq;

import com.rabbitmq.client.Channel;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.statemachine.FileProcessingEvent;
import com.trina.visiontask.statemachine.FileProcessingService;
import com.trina.visiontask.statemachine.FileProcessingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pdf-convert.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class PdfConvertConsumer {
    private static final Logger log = LoggerFactory.getLogger(PdfConvertConsumer.class);
    private final FileProcessingService fileProcessingService;

    public PdfConvertConsumer(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @RabbitListener(id = "pdf-convert.consumer", queues = "${pdf-convert.consumer.queue-name}")
    public void consumeMessage(Channel channel, TaskDTO taskInfo, Message message) throws Exception {
        log.info("========> received pdf convert message");
        try {
            // 处理消息的业务逻辑
            processMessage(taskInfo);
        } catch (Exception e) {
            log.warn("consume pdf convert message failed, {}", e.getMessage());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("<======= Finished processing pdf convert message");
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(TaskDTO message) throws Exception {
        message.setTaskType("pdf-convert");
        fileProcessingService.processFile(
                FileProcessingState.UPLOADED,
                FileProcessingEvent.PDF_CONVERT_START,
                message);
    }
}