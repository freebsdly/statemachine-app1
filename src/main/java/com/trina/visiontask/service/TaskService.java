package com.trina.visiontask.service;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.repository.FileRepository;
import com.trina.visiontask.repository.TaskRepository;
import com.trina.visiontask.repository.entity.FileEntity;
import com.trina.visiontask.repository.entity.TaskEntity;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final DtoMapper dtoMapper;
    private final ObjectStorageService objectStorageService;
    private final MessageProducer messageProducer;
    private final EntityManager entityManager;

    public TaskService(FileRepository fileRepository,
                       TaskRepository taskRepository,
                       DtoMapper dtoMapper,
                       ObjectStorageService objectStorageService,
                       MessageProducer messageProducer,
                       EntityManager entityManager
    ) {
        this.fileRepository = fileRepository;
        this.taskRepository = taskRepository;
        this.dtoMapper = dtoMapper;
        this.objectStorageService = objectStorageService;
        this.messageProducer = messageProducer;
        this.entityManager = entityManager;
    }


    @Transactional
    public void saveTask(TaskDTO taskInfo) throws Exception {
        Optional<FileEntity> file = fileRepository.findByFileId(taskInfo.getFileInfo().getFileId());
        FileEntity fileEntity;
        if (file.isPresent()) {
            fileEntity = file.get();
            fileEntity = dtoMapper.partialUpdate(taskInfo.getFileInfo(), fileEntity);
            fileEntity = fileRepository.save(fileEntity);
        } else {
            fileEntity = fileRepository.save(dtoMapper.toEntity(taskInfo.getFileInfo()));
        }
        TaskEntity taskEntity = dtoMapper.toEntity(taskInfo);
        taskEntity.setFileInfo(fileEntity);
        taskRepository.save(taskEntity);
    }

    @Transactional
    public void saveOrUpdateTask(TaskDTO taskInfo) {
        Optional<FileEntity> file = fileRepository.findByFileId(taskInfo.getFileInfo().getFileId());
        FileEntity fileEntity;
        if (file.isPresent()) {
            fileEntity = file.get();
            fileEntity = dtoMapper.partialUpdate(taskInfo.getFileInfo(), fileEntity);
        } else {
            fileEntity = dtoMapper.toEntity(taskInfo.getFileInfo());
        }
        fileEntity = entityManager.merge(fileEntity);
        entityManager.flush();
        Optional<TaskEntity> task = taskRepository.findByTaskId(taskInfo.getTaskId());
        TaskEntity taskEntity;
        if (task.isPresent()) {
            taskEntity = task.get();
            taskEntity = dtoMapper.partialUpdate(taskInfo, taskEntity);
        } else {
            taskEntity = dtoMapper.toEntity(taskInfo);
        }
        taskEntity.setFileInfo(fileEntity);
        taskRepository.saveAndFlush(taskEntity);
    }

    @Transactional
    public void updateTask(TaskDTO taskInfo) throws Exception {
        TaskEntity taskEntity = taskRepository.findByTaskId(taskInfo.getTaskId())
                .orElseThrow();
        dtoMapper.partialUpdate(taskInfo, taskEntity);
        taskRepository.save(taskEntity);
    }

    public TaskDTO getTask(UUID taskId) throws Exception {
        TaskEntity task = taskRepository.findByTaskId(taskId).orElseThrow(
                () -> new Exception(String.format("task %s not found", taskId))
        );
        return dtoMapper.toDto(task);
    }

    public TaskDTO uploadFile(MultipartFile file, boolean force) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("file is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("file name is empty");
        }

        FileDTO fileInfo = new FileDTO();
        Optional<FileEntity> exist = fileRepository.findByFileName(file.getOriginalFilename());
        boolean shouldUpload = false;

        if (exist.isPresent()) {
            fileInfo = dtoMapper.toDto(exist.get());
            if (file.getSize() != fileInfo.getFileSize()) {
                shouldUpload = true;
            }
        } else {
            shouldUpload = true;
            String suffix = null;
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                suffix = originalFilename.substring(i + 1);
            }

            fileInfo.setFileId(UUID.randomUUID());
            fileInfo.setFileName(originalFilename);
            fileInfo.setMimeType(file.getContentType());
            fileInfo.setFileType(suffix);
        }

        if (force) {
            log.debug("force to upload file {}", originalFilename);
            shouldUpload = true;
        }


        if (shouldUpload) {
            String uploadName = String.format("%s.%s", fileInfo.getFileId(), fileInfo.getFileType());
            Optional<CompleteMultipartUploadResult> result = objectStorageService.upload(uploadName, file.getInputStream()).blockOptional();
            if (result.isEmpty()) {
                log.error("upload file {} failed", fileInfo.getFileName());
                throw new Exception("upload file failed");
            }
            CompleteMultipartUploadResult ossObject = result.get();
            fileInfo.setOssFileKey(ossObject.getKey());
            fileInfo.setFilePath(ossObject.getLocation());
            fileInfo.setFileSize(file.getSize());
        } else {
            throw new Exception("file already exists and do not need to update");
        }

        // 构造TaskInfo对象
        TaskDTO taskInfo = new TaskDTO();
        taskInfo.setTaskId(UUID.randomUUID());
        taskInfo.setFileInfo(fileInfo);
        taskInfo.setStartTime(LocalDateTime.now());
        taskInfo.setEndTime(LocalDateTime.now());

        // 发送到处理队列, 文件已经上传这里设置初始状态发送给处理队列更新状态
        taskInfo.setCurrentState(FileProcessingState.INITIAL);
        taskInfo.setEvent(FileProcessingEvent.UPLOAD_START);
        messageProducer.sendToUploadQueue(taskInfo);
        return taskInfo;
    }
}
