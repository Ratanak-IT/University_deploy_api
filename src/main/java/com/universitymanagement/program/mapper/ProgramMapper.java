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
    // Counts are filled in by the service from grouped queries; mapping them off
    // the entity's collections would load every subject and student per row.
    @Mapping(target = "subjectCount", ignore = true)
    @Mapping(target = "studentCount", ignore = true)
    ProgramResponse toResponse(Program program);
    Program toEntity(ProgramRequest programRequest);
}
