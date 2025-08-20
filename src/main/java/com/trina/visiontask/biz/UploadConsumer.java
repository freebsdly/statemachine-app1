package com.trina.visiontask.biz;

import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@AllArgsConstructor(onConstructor_ = @Autowired)
@ConditionalOnProperty(name = "upload.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class UploadConsumer
{

    private final FileProcessingService fileProcessingService;


    @RabbitListener(id = "upload.consumer", queues = "${upload.consumer.queue.name}")
    public void consumeMessage(Channel channel, FileInfo fileInfo, Message message) throws IOException
    {
        try {
            // 处理消息的业务逻辑
            processMessage(fileInfo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.warn("consume message failed, {}", e.getMessage());
            // 可以根据需要进行消息重试或死信队列处理
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(FileInfo message)
    {
        try {
            fileProcessingService.processFile(FileProcessingState.INITIAL, FileProcessingEvent.UPLOAD_START, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
