package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UploadConsumer
{

    @RabbitListener(queues = "${upload.consumer.queue.name}")
    public void consumeMessage(FileInfo message)
    {
        try {
            // 处理消息的业务逻辑
            processMessage(message);
        } catch (Exception e) {
            log.warn("处理消息时发生异常: {}", e.getMessage());
            // 可以根据需要进行消息重试或死信队列处理
        }
    }

    /**
     * 处理消息的业务逻辑, 串行执行
     *
     * @param message 消息内容
     */
    private void processMessage(FileInfo message)
    {
        log.info("处理消息: {}", message);
        try {
            // 模拟处理消息的耗时
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
