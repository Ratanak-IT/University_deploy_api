package com.universitymanagement.program.mapper;

import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.program.entity.Program;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgramMapper {
    @Mapping(target = "departmentId", source = "department.departmentId")
    @Mapping(target = "departmentName", source = "department.departmentName")
    ProgramResponse toResponse(Program program);
    Program toEntity(ProgramRequest programRequest);
}
