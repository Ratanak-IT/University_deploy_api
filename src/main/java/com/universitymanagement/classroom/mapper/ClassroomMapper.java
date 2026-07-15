package com.universitymanagement.classroom.mapper;

import com.universitymanagement.classroom.dto.request.ClassroomCreateRequest;
import com.universitymanagement.classroom.dto.request.ClassroomUpdateRequest;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClassroomMapper {
    @Mapping(target = "teacherId", source = "teacher.teacherId")
    @Mapping(target = "teacherName", source = "teacher.user.fullName")
    @Mapping(target = "subjectId", source = "subject.subjectId")
    @Mapping(target = "subjectName", source = "subject.subjectName")
    @Mapping(target = "programId", source = "program.id")
    @Mapping(target = "programName", source = "program.programName")
    @Mapping(target = "updatedAt", source = "lastUpdateAt")
    @Mapping(target = "updatedBy", source = "lastUpdatedBy")
    ClassroomResponse toResponse(Classroom classroom);
    Classroom toEntity(ClassroomCreateRequest classroomCreateRequest);
    @Mapping(target = "studentId", source = "student.studentId")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "fullName", source = "student.user.fullName")
    @Mapping(target = "email", source = "student.user.email")
    @Mapping(target = "yearLevel", source = "student.yearLevel")
    @Mapping(target = "semester", source = "student.semester")
    @Mapping(target = "joinedAt", source = "createdAt")
    ClassroomStudentResponse toStudentResponse(ClassroomStudent classroomStudent);
    void updateEntity(ClassroomUpdateRequest request, @MappingTarget Classroom classroom);
}
