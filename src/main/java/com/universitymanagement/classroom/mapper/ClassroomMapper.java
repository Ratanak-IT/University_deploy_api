package com.universitymanagement.classroom.mapper;

import com.universitymanagement.classroom.dto.request.ClassroomCreateRequest;
import com.universitymanagement.classroom.dto.request.ClassroomUpdateRequest;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClassroomMapper {

    ClassroomResponse toResponse(Classroom classroom);
    Classroom toEntity(ClassroomCreateRequest classroomCreateRequest);
    ClassroomStudentResponse toStudentResponse(ClassroomStudent classroomStudent);
    void updateEntity(ClassroomUpdateRequest request, @MappingTarget Classroom classroom);
}
