package com.trina.visiontask.biz;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "md-convert.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class MarkdownConvertConsumer {
    private static final Logger log = LoggerFactory.getLogger(MarkdownConvertConsumer.class);
    private final FileProcessingService fileProcessingService;

    public MarkdownConvertConsumer(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @RabbitListener(id = "md-convert.consumer", queues = "${md-convert.consumer.queue-name}")
    public void consumeMessage(Channel channel, TaskInfo taskInfo, Message message) throws Exception {
        log.info("=======> received md convert message");
        try {
            // 处理消息的业务逻辑
            processMessage(taskInfo);
        } catch (Exception e) {
            log.warn("consume md convert message failed, {}", e.getMessage());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("<======== finished process md convert message");
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(TaskInfo message) throws Exception {
        message.setTaskType("md-convert");
        fileProcessingService.processFile(
                FileProcessingState.PDF_CONVERTED,
                FileProcessingEvent.MD_CONVERT_START,
                message);
    }
}
