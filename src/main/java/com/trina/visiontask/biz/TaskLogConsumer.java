package com.trina.visiontask.biz;

import com.trina.visiontask.repository.FileRepository;
import com.trina.visiontask.repository.TaskRepository;
import com.trina.visiontask.repository.entity.FileEntity;
import com.trina.visiontask.repository.entity.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "task-log.consumer.enabled", havingValue = "true", matchIfMissing = false)
public class TaskLogConsumer {
    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final BizMapper bizMapper;

    public TaskLogConsumer(FileRepository fileRepository,
                           TaskRepository taskRepository,
                           BizMapper bizMapper) {
        this.fileRepository = fileRepository;
        this.taskRepository = taskRepository;
        this.bizMapper = bizMapper;
    }

    @RabbitListener(id = "task-log.consumer", queues = "${task-log.consumer.queue-name}", containerFactory = "taskLogContainerFactory")
    public void consumeMessage(TaskInfo taskInfo) throws Exception {
        log.info("received task log message: {}", taskInfo);
        Optional<FileEntity> file = fileRepository.findByFileId(taskInfo.getFileInfo().getFileId());
        FileEntity fileEntity;
        if (file.isPresent()) {
            fileEntity = file.get();
            bizMapper.partialUpdate(taskInfo.getFileInfo(), fileEntity);
            fileEntity = fileRepository.save(fileEntity);
        } else {
            fileEntity = fileRepository.save(bizMapper.toEntity(taskInfo.getFileInfo()));
        }
        TaskEntity taskEntity = bizMapper.toEntity(taskInfo);
        taskEntity.setFileInfo(fileEntity);
        taskRepository.save(taskEntity);
    }
}
