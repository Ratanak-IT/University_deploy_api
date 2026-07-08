package com.universitymanagement.classroom.mapper;

import com.universitymanagement.classroom.dto.request.ClassroomRequest;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.entity.Classroom;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClassroomMapper {

    ClassroomResponse toResponse(Classroom classroom);
    Classroom toEntity(ClassroomRequest classroomRequest);
}
