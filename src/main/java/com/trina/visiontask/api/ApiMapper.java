package com.trina.visiontask.api;

import com.trina.visiontask.statemachine.CallbackInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApiMapper {

    CallbackInfo to(CallbackDTO dto);
}
