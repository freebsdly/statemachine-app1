package com.trina.visiontask.mq;

import com.rabbitmq.client.Channel;
import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.statemachine.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "md-convert.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class MarkdownConvertConsumer {
    private static final Logger log = LoggerFactory.getLogger(MarkdownConvertConsumer.class);
    private final FileProcessingService fileProcessingService;
    private final String callbackUrl;

    public MarkdownConvertConsumer(FileProcessingService fileProcessingService,
                                   @Qualifier("getMdCallbackUrl") String callbackUrl) {
        this.fileProcessingService = fileProcessingService;
        this.callbackUrl = callbackUrl;

    }

    @RabbitListener(id = "md-convert.consumer", queues = "${md-convert.consumer.queue-name}")
    public void consumeMessage(Channel channel, TaskDTO taskInfo, Message message) throws Exception {
        log.debug("=======> received md convert message");
        try {
            // 处理消息的业务逻辑
            processMessage(taskInfo);
        } catch (Exception e) {
            log.warn("consume md convert message failed, {}", e.getMessage());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.debug("<======== finished process md convert message");
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(TaskDTO message) throws Exception {
        message.setTaskType("md-convert");
        message.setMdCallbackUrl(callbackUrl);
        fileProcessingService.processFile(
                FileProcessingState.PDF_CONVERTED,
                FileProcessingEvent.MD_CONVERT_START,
                message);
    }
}
