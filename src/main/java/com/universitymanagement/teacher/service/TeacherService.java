package com.universitymanagement.teacher.service;

import com.universitymanagement.classroom.dto.response.ClassroomResponse;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import com.universitymanagement.teacher.dto.request.AssignClassroomRequest;
import com.universitymanagement.teacher.dto.request.AssignSubjectRequest;
import com.universitymanagement.teacher.dto.request.CreateTeacherRequest;
import com.universitymanagement.teacher.dto.request.UpdateTeacherRequest;
import com.universitymanagement.teacher.dto.response.TeacherDetailResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface TeacherService {

    Page<TeacherResponse> getAllTeachers(int page, int size);

    TeacherResponse getTeacherById(UUID teacherId);

    TeacherDetailResponse findTeacherByUserId(String userId);

    TeacherResponse createTeacher(CreateTeacherRequest request);

    TeacherResponse updateTeacher(UUID teacherId, UpdateTeacherRequest request);

    void deleteTeacher(UUID teacherId);

    List<ClassroomResponse> getAssignedClasses(UUID teacherId);

    TeacherResponse assignSubject(UUID teacherId, AssignSubjectRequest request);

    TeacherResponse unassignSubject(UUID teacherId, UUID subjectId);

    List<SubjectResponse> getAssignedSubjects(UUID teacherId);

    ClassroomResponse assignClassroom(UUID teacherId, AssignClassroomRequest request);
    TeacherResponse assignDepartment(UUID teacherId, UUID departmentId);

    TeacherResponse unassignDepartment(UUID teacherId, UUID departmentId);
}
