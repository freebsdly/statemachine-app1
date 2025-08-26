package com.trina.visiontask.biz;

import com.trina.visiontask.repository.entity.FileEntity;
import com.trina.visiontask.repository.entity.TaskEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = {BizMapper.class})

public interface BizMapper {
    FileEntity toEntity(FileInfo fileInfo);


    FileInfo toDto(FileEntity fileEntity);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileEntity partialUpdate(FileInfo fileInfo, @MappingTarget FileEntity fileEntity);

    TaskEntity toEntity(TaskInfo taskInfo);


    TaskInfo toDto(TaskEntity taskEntity);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    TaskEntity partialUpdate(TaskInfo taskInfo, @MappingTarget TaskEntity taskEntity);
}
