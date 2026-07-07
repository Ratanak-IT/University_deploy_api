package com.universitymanagement.teacher.service;

import com.universitymanagement.teacher.dto.response.TeacherDetailResponse;

public interface TeacherService {

    TeacherDetailResponse findTeacherById(String userId);
}
