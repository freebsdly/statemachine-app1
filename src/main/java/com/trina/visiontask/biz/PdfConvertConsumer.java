package com.trina.visiontask.biz;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "pdf-convert.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class PdfConvertConsumer {

    private final FileProcessingService fileProcessingService;

    public PdfConvertConsumer(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @RabbitListener(id = "pdf-convert.consumer", queues = "${pdf-convert.consumer.queue-name}")
    public void consumeMessage(Channel channel, TaskInfo taskInfo, Message message) throws Exception {
        log.info("received pdf convert message: {}", taskInfo);
        try {
            // 处理消息的业务逻辑
            processMessage(taskInfo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.warn("consume pdf convert message failed, {}", e.getMessage());
            // 可以根据需要进行消息重试或死信队列处理
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(TaskInfo message) throws Exception {
        fileProcessingService.processFile(
                FileProcessingState.UPLOADED,
                FileProcessingEvent.PDF_CONVERT_START,
                message);
    }
}