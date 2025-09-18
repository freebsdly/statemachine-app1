package com.trina.visiontask.mq;

import com.trina.visiontask.MQConfiguration;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "task-log.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class TaskLogConsumer {
    private static final Logger log = LoggerFactory.getLogger(TaskLogConsumer.class);
    private final TaskService taskService;
    private final MQConfiguration mqConfig;

    public TaskLogConsumer(
            TaskService taskService,
            @Qualifier("taskLogConfiguration") MQConfiguration mqConfig
    ) {
        this.taskService = taskService;
        this.mqConfig = mqConfig;
    }

    @RabbitListener(id = "task-log.consumer", queues = {"${task-log.consumer.queue-name}"}, containerFactory = "taskLogContainerFactory")
    public void consumeMessage(TaskDTO taskInfo) throws Exception {
        log.debug("received task log message: {}", taskInfo);
        try {
            // 这里只更新ossFile的信息
            taskService.updateOssFile(taskInfo);
            taskService.saveTaskHistory(taskInfo);
        } catch (Exception e) {
            log.error("save task history log failed", e);
        }
    }
}
