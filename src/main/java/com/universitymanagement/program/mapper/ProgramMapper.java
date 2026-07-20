package com.universitymanagement.program.mapper;

import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.program.entity.Program;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProgramMapper {
    ProgramResponse toResponse(Program program);
    Program toEntity(ProgramRequest programRequest);
}
