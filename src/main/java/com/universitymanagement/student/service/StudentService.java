package com.universitymanagement.student.service;

import com.universitymanagement.student.dto.response.StudentDetailResponse;

public interface StudentService {
    StudentDetailResponse findStudentById(String userId);
}
