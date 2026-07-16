package com.universitymanagement.student.service;

import com.universitymanagement.student.dto.request.CreateStudentRequest;
import com.universitymanagement.student.dto.request.StudentUpdateProfileRequest;
import com.universitymanagement.student.dto.request.StudentUpdateRequest;
import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.dto.response.StudentDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StudentService {

    // for admin
    Page<StudentAdminResponse> getAllStudents(int page, int size, String keyword);

    StudentAdminResponse getStudentById(UUID studentId);

    StudentAdminResponse createStudent(CreateStudentRequest request);

    StudentAdminResponse updateStudent(UUID studentId, StudentUpdateRequest request);

    void deleteStudent(UUID studentId);

    StudentDetailResponse getMyProfile();

    StudentDetailResponse updateMyProfile(StudentUpdateProfileRequest request);
    StudentDetailResponse findStudentById(String keycloakUserId);
    StudentDetailResponse uploadMyAvatar(MultipartFile file);
}
