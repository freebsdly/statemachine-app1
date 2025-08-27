package com.trina.visiontask.service;

import com.trina.visiontask.repository.entity.FileEntity;
import com.trina.visiontask.repository.entity.TaskEntity;
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
}
