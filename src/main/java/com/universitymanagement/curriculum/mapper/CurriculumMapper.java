package com.universitymanagement.curriculum.mapper;

import com.universitymanagement.curriculum.dto.request.CurriculumRequest;
import com.universitymanagement.curriculum.dto.response.CurriculumResponse;
import com.universitymanagement.curriculum.entity.Curriculum;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurriculumMapper {

CurriculumResponse toResponse(Curriculum curriculum);
Curriculum toEntity(CurriculumRequest curriculumRequest);
}
