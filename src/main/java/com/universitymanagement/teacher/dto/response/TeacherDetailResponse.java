package com.universitymanagement.teacher.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TeacherDetailResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        List<String> roles,
        String teacherCode,
        String department,
        String position,
        String specialization,
        LocalDate hireDate,
        String employmentStatus
) {
}
