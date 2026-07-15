package com.universitymanagement.classroom.service;

import com.universitymanagement.classroom.dto.request.*;
import com.universitymanagement.classroom.dto.response.ClassroomMemberResponse;
import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.classroom.dto.response.ClassroomStudentResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ClassroomService {
    Page<ClassroomResponse> getAllClassrooms(int page, int size);

    ClassroomResponse createClassroom(ClassroomCreateRequest request);

    ClassroomResponse updateClassroom(UUID classroomId, ClassroomUpdateRequest request);

    void softDelete(UUID classroomId);

    void addStudentsToClassroom(UUID classroomId, AddStudentsRequest request);

    void removeStudentFromClassroom(UUID classroomId, UUID studentId);

    ClassroomResponse getClassroomById(UUID classroomId);

    List<ClassroomStudentResponse> getStudentsInClassroom(UUID classroomId);

    List<ClassroomResponse> getMyClassrooms();

    ClassroomMemberResponse addTeacherToClassroom(UUID classroomId, AssignTeacherRequest request);

    List<ClassroomMemberResponse> getTeachersInClassroom(UUID classroomId);

    void removeTeacherFromClassroom(UUID classroomId, UUID teacherId);

    ClassroomResponse setLeadTeacher(UUID classroomId, AssignTeacherRequest request);
}
