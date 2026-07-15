package com.universitymanagement.teacher.dto.response;

import com.universitymanagement.subject.dto.response.SubjectResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TeacherResponse(
        UUID teacherId,
        String teacherCode,
        String fullName,
        String email,
        String phoneNumber,
        String specialization,
        List<TeacherDepartmentResponse> departments,
        String position,
        LocalDate hireDate,
        String employmentStatus,
        List<SubjectResponse> subjects
) {
}
