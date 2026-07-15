package com.universitymanagement.curriculum.mapper;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.entity.Curriculum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CurriculumMapper {
    @Mapping(source = "program.id", target = "programId")
    @Mapping(source = "program.programName", target = "programName")
    @Mapping(source = "subject.subjectId", target = "subjectId")
    @Mapping(source = "subject.subjectName", target = "subjectName")
    @Mapping(source = "subject.subjectCode", target = "subjectCode")
    @Mapping(source = "subject.credit", target = "credit")
    CurriculumResponse toResponse(Curriculum curriculum);

    Curriculum toEntity(CurriculumRequest curriculumRequest);
}
