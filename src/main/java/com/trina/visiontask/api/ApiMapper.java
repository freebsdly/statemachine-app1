package com.trina.visiontask.api;

import com.trina.visiontask.service.*;
import com.trina.visiontask.statemachine.CallbackInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApiMapper {

    CallbackInfo to(CallbackDTO dto);

    FileQueryOptions to(FileQueryDTO dto);

    TaskQueryOptions to(TaskQueryDTO dto);

    PageDTO<TaskDTO> to(Page<TaskDTO> page);

    UploadDTO to(UploadDocumentDTO dto);
}
