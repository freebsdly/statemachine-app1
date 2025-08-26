package com.trina.visiontask.repository.entity;

import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = {EntityMapper.class})
public interface EntityMapper {
    FileEntity toEntity(FileDto fileDto);

    FileDto toDto(FileEntity fileEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileEntity partialUpdate(FileDto fileDto, @MappingTarget FileEntity fileEntity);

    TaskEntity toEntity(TaskDto taskDto);

    TaskDto toDto(TaskEntity taskEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    TaskEntity partialUpdate(TaskDto taskDto, @MappingTarget TaskEntity taskEntity);
}