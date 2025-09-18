package com.trina.visiontask.service;

import com.trina.visiontask.repository.entity.FileEntity;
import com.trina.visiontask.repository.entity.OssFileEntity;
import com.trina.visiontask.repository.entity.TaskEntity;
import com.trina.visiontask.repository.entity.TaskHistoryEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)

public interface DtoMapper {
    FileEntity toEntity(FileDTO fileDTO);

    FileDTO toDto(FileEntity fileEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileEntity partialUpdate(FileDTO fileDTO, @MappingTarget FileEntity fileEntity);

    TaskEntity toEntity(TaskDTO taskDTO);

    TaskDTO toDto(TaskEntity taskEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    TaskEntity partialUpdate(TaskDTO taskDTO, @MappingTarget TaskEntity taskEntity);

    TaskHistoryEntity toHistoryEntity(TaskDTO taskDTO);

    default OssFileEntity toOssFile(TaskDTO dto) {
        if (dto == null) {
            return null;
        }

        OssFileEntity entity = new OssFileEntity();
        FileDTO fileInfo = dto.getFileInfo();
        if (fileInfo != null) {
            String prefix = null;
            int i = fileInfo.getFileName().lastIndexOf('.');
            if (i > 0) {
                prefix = fileInfo.getFileName().substring(0, i);
            } else {
                prefix = fileInfo.getFileName();
            }
            entity.setId(fileInfo.getFileId().toString());
            entity.setTankId(fileInfo.getTankId());
            entity.setFileName(prefix);
            entity.setSize(fileInfo.getFileSize());
            entity.setFileSuffix(fileInfo.getFileType());
            entity.setUrl(fileInfo.getOssFileKey());
            entity.setUploadTime(dto.getStartTime());
            entity.setMessage(dto.getMessage());
            entity.setEmpno(dto.getOperator());
        }
        return entity;
    }
}
