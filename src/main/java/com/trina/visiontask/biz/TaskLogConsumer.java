package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "task-log.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class TaskLogConsumer {

    public TaskLogConsumer() {
    }

    @RabbitListener(id = "task-log.consumer", queues = "${task-log.consumer.queue-name}", containerFactory = "taskLogContainerFactory")
    public void consumeMessage(TaskInfo taskInfo) throws Exception {
        log.info("received task log message: {}", taskInfo);
    }
}
