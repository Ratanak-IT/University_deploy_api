package com.universitymanagement.subject.mapper;

import com.universitymanagement.subject.dto.request.SubjectRequest;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.subject.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    @Mapping(target = "departmentId", source = "department.departmentId")
    SubjectResponse toResponse(Subject subject);
    @Mapping(target = "department", ignore = true)
    Subject toEntity(SubjectRequest subjectRequest);
}
