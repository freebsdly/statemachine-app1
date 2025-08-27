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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai-slice.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class AiSliceConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiSliceConsumer.class);

    private final FileProcessingService fileProcessingService;
    private final String callbackUrl;

    public AiSliceConsumer(FileProcessingService fileProcessingService,
                           @Qualifier("getSliceCallbackUrl") String callbackUrl) {
        this.fileProcessingService = fileProcessingService;
        this.callbackUrl = callbackUrl;
    }

    @RabbitListener(id = "ai-slice.consumer", queues = "${ai-slice.consumer.queue-name}")
    public void consumeMessage(Channel channel, TaskDTO taskInfo, Message message) throws Exception {
        log.info("=====> received ai slice message");
        try {
            // 处理消息的业务逻辑
            processMessage(taskInfo);
        } catch (Exception e) {
            log.warn("consume ai slice message failed, {}", e.getMessage());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("<======= Finished processing ai slice message: {}", taskInfo);
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(TaskDTO message) throws Exception {
        message.setTaskType("ai-slice");
        message.setSliceCallbackUrl(callbackUrl);
        fileProcessingService.processFile(
                FileProcessingState.MARKDOWN_CONVERTED,
                FileProcessingEvent.AI_SLICE_START,
                message);

    }
}
