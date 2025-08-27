package com.trina.visiontask.biz;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.trina.visiontask.repository.FileRepository;
import com.trina.visiontask.repository.TaskRepository;
import com.trina.visiontask.repository.entity.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class FileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final BizMapper bizMapper;
    private final ObjectStorageService objectStorageService;
    private final StateMachineManager stateMachineManager;
    private final MessageProducer messageProducer;
    private final String taskInfoKey;
    private final long waitTimeout;

    public FileProcessingService(
            FileRepository fileRepository,
            TaskRepository taskRepository,
            BizMapper bizMapper,
            ObjectStorageService objectStorageService,
            StateMachineManager stateMachineManager,
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("waitTimeout") long timeout
    ) {
        this.fileRepository = fileRepository;
        this.taskRepository = taskRepository;
        this.bizMapper = bizMapper;
        this.objectStorageService = objectStorageService;
        this.stateMachineManager = stateMachineManager;
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
        this.waitTimeout = timeout;
    }

    public TaskInfo uploadFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("file is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("file name is empty");
        }

        FileInfo fileInfo = new FileInfo();
        Optional<FileEntity> exist = fileRepository.findByFileName(file.getOriginalFilename());
        boolean shouldUpload = false;
        if (exist.isPresent()) {
            fileInfo = bizMapper.toDto(exist.get());
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
        TaskInfo taskInfo = new TaskInfo();
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

    public void processFile(FileProcessingState initState, FileProcessingEvent event, TaskInfo taskInfo)
            throws Exception {
        // 这里使用builder创建状态机，以便从指定的初始状态开始，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = stateMachineManager.acquireStateMachine(initState, taskInfo.getTaskId().toString());
        CountDownLatch completionLatch = new CountDownLatch(1);
        if (taskInfo.getStartTime() == null) {
            taskInfo.setStartTime(LocalDateTime.now());
        }
        StateMachineListenerAdapter<FileProcessingState, FileProcessingEvent> listener = new StateMachineListenerAdapter<>() {
            private StateContext<FileProcessingState, FileProcessingEvent> context;

            @Override
            public void stateContext(StateContext<FileProcessingState, FileProcessingEvent> stateContext) {
                this.context = stateContext;
            }

            @Override
            public void stateChanged(State<FileProcessingState, FileProcessingEvent> from, State<FileProcessingState, FileProcessingEvent> to) {
                if (from == null) {
                    // 初始化状态
                    return;
                }
                FileProcessingState source = from.getId();
                FileProcessingState target = to.getId();
                FileProcessingEvent event = context.getEvent();
                TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
                log.info("State changed from {} to {}, triggered by {}", source, target, event);
                if (taskInfo != null) {
                    taskInfo.setPreviousState(source);
                    taskInfo.setCurrentState(target);
                    taskInfo.setEvent(event);
                    taskInfo.setEndTime(LocalDateTime.now());
                }
                messageProducer.sendToTaskLogQueue(taskInfo);
                switch (target) {
                    case UPLOADED -> messageProducer.sendToPdfConvertQueue(taskInfo);
                    case PDF_CONVERTED -> messageProducer.sendToMdConvertQueue(taskInfo);
                    case MARKDOWN_CONVERTED -> messageProducer.sendToAiSliceQueue(taskInfo);
                }
                // 检查是否到达可退出状态
                if (target == FileProcessingState.UPLOADED
                        || target == FileProcessingState.PDF_CONVERTED
                        || target == FileProcessingState.MARKDOWN_CONVERTED
                        || target == FileProcessingState.COMPLETED
                        || target == FileProcessingState.FAILED
                ) {
                    completionLatch.countDown();
                } else if (target == FileProcessingState.MARKDOWN_CONVERT_SUBMITTED
                        || target == FileProcessingState.AI_SLICE_SUBMITTED
                ) {
                    log.info("waiting for markdown or slice callback");
                }
            }
        };
        stateMachine.addStateListener(listener);
        stateMachine.startReactively().subscribe();
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader(taskInfoKey, taskInfo)
                .build();
        // 发送初始事件，开始文件处理流程
        stateMachine.sendEvent(Mono.just(message)).subscribe();

        // 等待处理完成
        boolean finished = completionLatch.await(waitTimeout, TimeUnit.SECONDS);
        if (finished) {
            log.info("File processing finished");
        } else {
            log.warn("File processing timed out");
        }
        stateMachineManager.releaseStateMachine(taskInfo.getTaskId().toString());
        stateMachine.stopReactively().subscribe();
    }
}