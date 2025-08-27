package com.trina.visiontask.mq;

import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "task-log.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class TaskLogConsumer {
    private static final Logger log = LoggerFactory.getLogger(TaskLogConsumer.class);
    private final TaskService taskService;

    public TaskLogConsumer(TaskService taskService) {
        this.taskService = taskService;
    }

    @RabbitListener(id = "task-log.consumer", queues = "${task-log.consumer.queue-name}", containerFactory = "taskLogContainerFactory")
    public void consumeMessage(TaskDTO taskInfo) throws Exception {
        log.info("received task log message: {}", taskInfo);
        taskService.saveTask(taskInfo);
    }
}
