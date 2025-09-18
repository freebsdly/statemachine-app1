package com.trina.visiontask.service;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.querydsl.core.BooleanBuilder;
import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.exception.Errors;
import com.trina.visiontask.repository.FileRepository;
import com.trina.visiontask.repository.OssFileRepository;
import com.trina.visiontask.repository.TaskHistoryRepository;
import com.trina.visiontask.repository.TaskRepository;
import com.trina.visiontask.repository.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final DtoMapper dtoMapper;
    private final ObjectStorageService objectStorageService;
    private final MessageProducer messageProducer;
    private final EntityManager entityManager;
    private final TaskConfiguration taskConfiguration;
    private final OssFileRepository ossFileRepository;

    public TaskService(FileRepository fileRepository,
                       TaskRepository taskRepository,
                       TaskHistoryRepository taskHistoryRepository,
                       DtoMapper dtoMapper,
                       ObjectStorageService objectStorageService,
                       MessageProducer messageProducer,
                       EntityManager entityManager,
                       TaskConfiguration taskConfiguration,
                       OssFileRepository ossFileRepository
    ) {
        this.fileRepository = fileRepository;
        this.taskRepository = taskRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.dtoMapper = dtoMapper;
        this.objectStorageService = objectStorageService;
        this.messageProducer = messageProducer;
        this.entityManager = entityManager;
        this.taskConfiguration = taskConfiguration;
        this.ossFileRepository = ossFileRepository;
    }

    @Transactional
    public TaskDTO saveOrUpdateTask(TaskDTO taskInfo) {
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
        return dtoMapper.toDto(taskEntity);
    }

    @Transactional
    public void saveOssFile(TaskDTO taskInfo) {
        OssFileEntity entity = dtoMapper.toOssFile(taskInfo);
        FileDTO fileInfo = taskInfo.getFileInfo();
        entity.setConvertTime(taskInfo.getEndTime());
        entity.setEmpno(taskInfo.getOperator());
        entity.setStatus(0L);
        entity.setSize(fileInfo.getFileSize());

        if (ossFileRepository.existsById(entity.getId())) {
            entityManager.merge(entity);
        } else {
            entityManager.persist(entity);
        }
        entityManager.flush();

        OssFileEntity entity1 = dtoMapper.toOssFile(taskInfo);
        entity1.setFileSuffix("pdf");
        entity1.setId(fileInfo.getPdfFileId().toString());
        String ossPDFKey = fileInfo.getOssFileKey().replace(fileInfo.getFileType(), "pdf");
        entity1.setUrl(ossPDFKey);
        entity1.setOriginalId(fileInfo.getFileId().toString());
        entity1.setEmpno(taskInfo.getOperator());
        entity1.setStatus(0L);
        entity1.setPdfStatus("0");

        Optional<OssFileEntity> exist = ossFileRepository.findByOriginalId(fileInfo.getFileId().toString());
        exist.ifPresent(entityManager::remove);
        entityManager.persist(entity1);
        entityManager.flush();
    }

    @Transactional
    public void resetOssFileInMDConvertStatus(UUID pdfFileId) throws Exception {
        OssFileEntity ossFileEntity = ossFileRepository.findById(pdfFileId.toString()).orElseThrow(
                () -> new Exception(String.format("pdf file %s not found", pdfFileId))
        );

        ossFileEntity.setStatus(0L);
        entityManager.merge(ossFileEntity);
        entityManager.flush();
    }

    @Transactional
    public void saveOssFileWithoutPdf(TaskDTO taskInfo) {
        OssFileEntity entity = dtoMapper.toOssFile(taskInfo);
        FileDTO fileInfo = taskInfo.getFileInfo();
        entity.setId(fileInfo.getPdfFileId().toString());
        entity.setConvertTime(taskInfo.getEndTime());
        entity.setEmpno(taskInfo.getOperator());
        entity.setStatus(0L);
        entity.setSize(fileInfo.getFileSize());

        Optional<OssFileEntity> exist = ossFileRepository.findByUrl(entity.getUrl());
        exist.ifPresent(entityManager::remove);
        entityManager.persist(entity);
        entityManager.flush();
    }

    @Transactional
    public void saveOssFileForReparse(TaskDTO taskInfo) {
        OssFileEntity entity = dtoMapper.toOssFile(taskInfo);
        FileDTO fileInfo = taskInfo.getFileInfo();
        entity.setId(fileInfo.getPdfFileId().toString());
        entity.setOriginalId(fileInfo.getFileId().toString());
        entity.setConvertTime(taskInfo.getEndTime());
        entity.setEmpno(taskInfo.getOperator());
        entity.setStatus(0L);
        entity.setSize(fileInfo.getFileSize());

        Optional<OssFileEntity> exist = ossFileRepository.findByUrl(entity.getUrl());
        exist.ifPresent(entityManager::remove);
        entityManager.persist(entity);
        entityManager.flush();
    }

    /**
     * oss_file表中，只有doc、ppt类文件有2条记录，其他只有1条记录
     *
     * @param taskInfo
     */
    @Transactional
    public void updateOssFile(TaskDTO taskInfo) {
        OssFileEntity entity = dtoMapper.toOssFile(taskInfo);
        FileDTO fileInfo = taskInfo.getFileInfo();
        FileProcessingEvent event = taskInfo.getEvent();
        String checkId = fileInfo.getPdfFileId().toString();
        entity.setId(checkId);
        if (!ossFileRepository.existsById(checkId)) {
            log.warn("pdf file record do not exist. id: {}", checkId);
            return;
        }
        if (fileInfo.getFileType().equalsIgnoreCase("pdf")) {
            switch (event) {
                case MD_CONVERT_SUCCESS -> {
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(1L);
                    entity.setPdfStatus("1");
                }
                case MD_CONVERT_FAILURE -> {
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(2L);
                    entity.setErrorCode(Errors.AI10002.getCode());
                    entity.setPdfStatus("1");
                }
                case AI_SLICE_SUCCESS -> {
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(3L);
                    entity.setPdfStatus("1");
                }
                case AI_SLICE_FAILURE -> {
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(4L);
                    entity.setErrorCode(Errors.AI10004.getCode());
                    entity.setPdfStatus("1");
                }
                default -> {
                    return;
                }
            }
        } else {
            switch (event) {
                case PDF_CONVERT_SUCCESS -> {
                    entity.setFileSuffix("pdf");
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setOriginalId(fileInfo.getFileId().toString());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(0L);
                    entity.setPdfStatus("1");
                }
                case PDF_CONVERT_FAILURE -> {
                    entity.setFileSuffix("pdf");
                    if (fileInfo.getOssPDFKey() == null || fileInfo.getOssPDFKey().isEmpty()) {
                        String ossFileKey = fileInfo.getOssFileKey();
                        String replace = replaceLastSuffix(ossFileKey, fileInfo.getFileType(), "pdf");
                        entity.setUrl(replace);
                    } else {
                        entity.setUrl(fileInfo.getOssPDFKey());
                    }
                    entity.setOriginalId(fileInfo.getFileId().toString());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setErrorCode(Errors.K10001.getCode());
                    entity.setStatus(0L);
                    // FIXME: 临时处理，对应系统设置失败重试次数，这里硬编码为3
                    if (taskInfo.getRetryCount() >= 3) {
                        entity.setPdfStatus("2");
                    } else {
                        entity.setPdfStatus("0");
                    }
                }
                case MD_CONVERT_SUCCESS -> {
                    entity.setFileSuffix("pdf");
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setOriginalId(fileInfo.getFileId().toString());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(1L);
                    entity.setPdfStatus("1");
                }
                case MD_CONVERT_FAILURE -> {
                    entity.setFileSuffix("pdf");
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setOriginalId(fileInfo.getFileId().toString());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(2L);
                    entity.setErrorCode(Errors.AI10002.getCode());
                    entity.setPdfStatus("1");
                }
                case AI_SLICE_SUCCESS -> {
                    entity.setFileSuffix("pdf");
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setOriginalId(fileInfo.getFileId().toString());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setStatus(3L);
                    entity.setPdfStatus("1");
                }
                case AI_SLICE_FAILURE -> {
                    entity.setFileSuffix("pdf");
                    entity.setUrl(fileInfo.getOssPDFKey());
                    entity.setOriginalId(fileInfo.getFileId().toString());
                    entity.setConvertTime(taskInfo.getEndTime());
                    entity.setEmpno(taskInfo.getOperator());
                    entity.setErrorCode(Errors.AI10004.getCode());
                    entity.setStatus(4L);
                    entity.setPdfStatus("1");
                }
                default -> {
                    return;
                }
            }
        }
        ossFileRepository.saveAndFlush(entity);
    }

    public String replaceLastSuffix(String fileName, String oldSuffix, String newSuffix) {
        if (fileName == null || oldSuffix == null || newSuffix == null) {
            return fileName;
        }

        int lastIndexOfDot = fileName.lastIndexOf("." + oldSuffix);
        if (lastIndexOfDot == -1) {
            return fileName; // 没有找到后缀
        }

        // 确保这个后缀是在文件名末尾
        if (lastIndexOfDot + oldSuffix.length() + 1 == fileName.length()) {
            return fileName.substring(0, lastIndexOfDot + 1) + newSuffix;
        }

        return fileName;
    }

    @Transactional
    public void saveTaskHistory(TaskDTO taskInfo) throws Exception {
        TaskHistoryEntity historyEntity = dtoMapper.toHistoryEntity(taskInfo);
        historyEntity.setId(null);
        taskHistoryRepository.saveAndFlush(historyEntity);
    }

    public TaskDTO getTask(UUID taskId) throws Exception {
        TaskEntity task = taskRepository.findByTaskId(taskId).orElseThrow(
                () -> new Exception(String.format("task %s not found", taskId))
        );
        return dtoMapper.toDto(task);
    }

    public FileDTO getFile(UUID fileId) throws Exception {
        FileEntity file = fileRepository.findByFileId(fileId).orElseThrow(
                () -> new Exception(String.format("file %s not found", fileId))
        );
        return dtoMapper.toDto(file);
    }

    @Transactional
    public TaskDTO uploadFile(UploadDTO dto) throws Exception {
        // 构造TaskInfo对象
        TaskDTO taskInfo = new TaskDTO();
        taskInfo.setTaskId(UUID.randomUUID());
        taskInfo.setStartTime(LocalDateTime.now());
        taskInfo.setTaskType("upload");
        taskInfo.setOperator(dto.getOperator());
        taskInfo.setCurrentState(FileProcessingState.INITIAL);
        taskInfo.setEvent(FileProcessingEvent.UPLOAD_START);

        MultipartFile file = dto.getFiledata();
        if (file.isEmpty()) {
            throw new Exception("file is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (dto.getFilename() != null && !dto.getFilename().isEmpty()) {
            originalFilename = dto.getFilename();
        }

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("file name is empty");
        }

        FileDTO fileInfo = new FileDTO();
        String parentPath = String.format("%s/%s", taskConfiguration.getUploadOssPrefix(), dto.getTankId());
        if (dto.getPath() != null && !dto.getPath().isEmpty() && !dto.getPath().equals("/")) {
            parentPath = String.format("%s/%s", parentPath, dto.getPath());

        }
        String uploadName = String.format("%s/%s", parentPath, originalFilename);
        Optional<FileEntity> exist = fileRepository.findByOssFileKey(uploadName);

        if (exist.isPresent()) {
            // 使用原文件信息
            fileInfo = dtoMapper.toDto(exist.get());
            fileInfo.setModifyTime(LocalDateTime.now());
        } else {
            String suffix = null;
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                suffix = originalFilename.substring(i + 1);
            }
            fileInfo.setFileId(UUID.randomUUID());
            fileInfo.setMimeType(file.getContentType());
            fileInfo.setFileType(suffix);
        }
        // 这里要主要处理直接在数据库修改文件信息的情况, 保证文件名时当前上传的文件名
        fileInfo.setFileName(originalFilename);
        fileInfo.setTankId(dto.getTankId());
        fileInfo.setParentPath(parentPath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setPdfFileId(taskInfo.getTaskId());

        Optional<CompleteMultipartUploadResult> result = objectStorageService.upload(uploadName, file.getInputStream()).blockOptional();
        if (result.isEmpty()) {
            log.error("upload file {} failed", fileInfo.getFileName());
            throw new Exception("upload file failed");
        }
        CompleteMultipartUploadResult ossObject = result.get();
        fileInfo.setOssFileKey(ossObject.getKey());
        fileInfo.setFilePath(ossObject.getLocation());

        taskInfo.setFileInfo(fileInfo);
        taskInfo.setEndTime(LocalDateTime.now());

        String fileType = fileInfo.getFileType();
        if (fileType.equalsIgnoreCase("docx")
                || fileType.equalsIgnoreCase("pptx")
                || fileType.equalsIgnoreCase("doc")
                || fileType.equalsIgnoreCase("ppt")
        ) {
            // 这里也上传1个空的pdf文件，保证listObjects能够显示文件
            InputStream emptyStream = new ByteArrayInputStream(new byte[10]);
            String pdfPlaceholder = replaceLastSuffix(uploadName, fileInfo.getFileType(), "pdf");
            result = objectStorageService.upload(pdfPlaceholder, emptyStream).blockOptional();
            if (result.isEmpty()) {
                log.error("upload file {} failed", fileInfo.getFileName());
                throw new Exception("upload file failed");
            }
            saveOssFile(taskInfo);
            messageProducer.sendToUploadQueue(taskInfo);
        } else if (fileType.equalsIgnoreCase("pdf")) {
            saveOssFileWithoutPdf(taskInfo);
            messageProducer.sendToUploadQueue(taskInfo);
        } else {
            saveOssFileWithoutPdf(taskInfo);
        }

        return taskInfo;
    }


    /**
     * 上传文件
     */
    @Transactional
    public TaskDTO reParsePdfFile(ReparseDTO dto) throws Exception {
        TaskDTO taskInfo = new TaskDTO();
        taskInfo.setTaskId(UUID.randomUUID());
        taskInfo.setStartTime(LocalDateTime.now());
        taskInfo.setTaskType("upload");
        taskInfo.setOperator(dto.getOperator());
        // 源文件已经存在，直接从pdf转换开始
        taskInfo.setCurrentState(FileProcessingState.INITIAL);
        taskInfo.setEvent(FileProcessingEvent.UPLOAD_START);

        UUID fileId = dto.getFileId();
        boolean originIsPdf = false;
        Optional<FileEntity> exist = fileRepository.findByPdfFileId(fileId);
        FileEntity fileEntity = null;
        if (exist.isPresent()) {
            fileEntity = exist.get();
        }

        if (fileEntity == null) {
            // 试试PDF文件
            exist = fileRepository.findByFileId(fileId);
            if (exist.isPresent()) {
                fileEntity = exist.get();
                originIsPdf = true;
            }
        }

        if (fileEntity == null) {
            // file_infos不存在记录，从oss_file获取
            Optional<OssFileEntity> byId = ossFileRepository.findById(dto.getFileId().toString());
            if (byId.isPresent()) {
                fileEntity = new FileEntity();
                OssFileEntity ossFileEntity = byId.get();
                if (ossFileEntity.getOriginalId() != null && !ossFileEntity.getOriginalId().isEmpty()) {
                    // 文件不是pdf，从oss_file获取
                    fileEntity.setFileId(UUID.fromString(ossFileEntity.getOriginalId()));
                    Optional<OssFileEntity> byId1 = ossFileRepository.findById(ossFileEntity.getOriginalId());
                    if (byId1.isPresent()) {
                        OssFileEntity originalOssFileEntity = byId1.get();
                        fileEntity.setFileType(originalOssFileEntity.getFileSuffix());
                        fileEntity.setFileName(originalOssFileEntity.getFileName());
                        fileEntity.setOssFileKey(originalOssFileEntity.getUrl());
                        fileEntity.setFileSize(originalOssFileEntity.getSize());
                        fileEntity.setCreateTime(originalOssFileEntity.getConvertTime());
                        String ossFilePath = String.format("%s/%s", objectStorageService.getOptions().getEndpoint(), originalOssFileEntity.getUrl());
                        fileEntity.setFilePath(ossFilePath);
                    } else {
                        throw new Exception(String.format("can not find original file by pdf file id %s", fileId));
                    }
                } else {
                    // 原文件是PDF文件
                    fileEntity.setFileId(UUID.randomUUID());
                    fileEntity.setFileType(ossFileEntity.getFileSuffix());
                    fileEntity.setFileName(ossFileEntity.getFileName());
                    fileEntity.setOssFileKey(ossFileEntity.getUrl());
                    fileEntity.setFileSize(ossFileEntity.getSize());
                    fileEntity.setCreateTime(ossFileEntity.getConvertTime());
                    String ossFilePath = String.format("%s/%s", objectStorageService.getOptions().getEndpoint(), ossFileEntity.getUrl());
                    fileEntity.setFilePath(ossFilePath);
                }

                fileEntity.setTankId(ossFileEntity.getTankId());
                String parentPath = Paths.get(ossFileEntity.getUrl()).getParent().toString();
                fileEntity.setParentPath(parentPath);
                fileEntity.setModifyTime(LocalDateTime.now());
                fileEntity.setStatus(FileProcessingState.UPLOADED);
            }
        }

        if (fileEntity == null) {
            throw new Exception(String.format("file with id %s not found", fileId));
        }

        FileDTO fileInfo = dtoMapper.toDto(fileEntity);
        fileInfo.setPdfFileId(taskInfo.getTaskId());

        taskInfo.setFileInfo(fileInfo);

        String fileType = fileInfo.getFileType();
        if (fileType.equalsIgnoreCase("docx")
                || fileType.equalsIgnoreCase("pptx")
                || fileType.equalsIgnoreCase("doc")
                || fileType.equalsIgnoreCase("ppt")
        ) {
            String uploadName = fileInfo.getOssFileKey();
            // 这里也上传1个空的pdf文件，保证listObjects能够显示文件
            InputStream emptyStream = new ByteArrayInputStream(new byte[10]);
            String pdfPlaceholder = replaceLastSuffix(uploadName, fileInfo.getFileType(), "pdf");
            Optional<CompleteMultipartUploadResult> result = objectStorageService.upload(pdfPlaceholder, emptyStream).blockOptional();
            if (result.isEmpty()) {
                log.error("upload file {} failed", fileInfo.getFileName());
                throw new Exception("upload file failed");
            }
            saveOssFile(taskInfo);
            messageProducer.sendToUploadQueue(taskInfo);
        } else if (fileType.equalsIgnoreCase("pdf")) {
            if (originIsPdf) {
                saveOssFileWithoutPdf(taskInfo);
            } else {
                saveOssFileForReparse(taskInfo);
            }
            messageProducer.sendToUploadQueue(taskInfo);
        } else {
            saveOssFileWithoutPdf(taskInfo);
        }

        return taskInfo;
    }

    public Page<FileDTO> getFiles(FileQueryOptions options) throws Exception {
        QFileEntity qFileEntity = QFileEntity.fileEntity;
        BooleanBuilder predicate = new BooleanBuilder();

        if (options.getFileIds() != null) {
            predicate.and(qFileEntity.fileId.in(options.getFileIds()));
        }
        if (options.getFileNames() != null) {
            predicate.and(qFileEntity.fileName.in(options.getFileNames()));
        }
        if (options.getFileTypes() != null) {
            predicate.and(qFileEntity.fileType.in(options.getFileTypes()));
        }
        if (options.getPath() != null) {
            predicate.and(qFileEntity.filePath.like(String.format("%s/%s", taskConfiguration.getUploadOssPrefix(), options.getPath())));
        } else {
            predicate.and(qFileEntity.filePath.like(String.format("%s", taskConfiguration.getUploadOssPrefix())));
        }
        PageRequest pageRequest = PageRequest.of(options.getPageNum(), options.getPageSize());
        return fileRepository.findAll(predicate, pageRequest).map(dtoMapper::toDto);
    }

    public Page<TaskDTO> getTasks(TaskQueryOptions options) throws Exception {
        QTaskEntity qTaskEntity = QTaskEntity.taskEntity;
        BooleanBuilder predicate = new BooleanBuilder();

        if (options.getTaskIds() != null) {
            predicate.and(qTaskEntity.taskId.in(options.getTaskIds()));
        }

        PageRequest pageRequest = PageRequest.of(options.getPageNum(), options.getPageSize());
        return taskRepository.findAll(predicate, pageRequest).map(dtoMapper::toDto);
    }

    /**
     * 绕过所有缓存直接查询数据库确认数据存在
     */
    public boolean verifyOssFileRecordExists(UUID uuid) {
        try {
            // 清除一级缓存
            entityManager.clear();

            // 使用原生SQL查询绕过所有缓存
            Query query = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM oss_file WHERE id = ?1");
            query.setParameter(1, uuid.toString());

            Number count = (Number) query.getSingleResult();
            return count.intValue() > 0;

        } catch (Exception e) {
            log.error("Error verifying OssFile record existence: {}", e.getMessage(), e);
            return false;
        }
    }

    // FIXME: 临时解决listObjects方法返回结果为空的问题
    public void checkOssFileRecord(TaskDTO dto) {
        String fileType = dto.getFileInfo().getFileType();
        if (fileType.equalsIgnoreCase("docx")
                || fileType.equalsIgnoreCase("pptx")
                || fileType.equalsIgnoreCase("doc")
                || fileType.equalsIgnoreCase("ppt")
        ) {
            UUID uuid = dto.getFileInfo().getPdfFileId();
            for (int i = 1; i <= 120; i++) {
                if (verifyOssFileRecordExists(uuid)) {
                    log.debug("OssFile record verified in database: {}", uuid);
                    break;
                } else {
                    log.debug("Waiting for OssFile record to persist, attempt {}/120", i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
