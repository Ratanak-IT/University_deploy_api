package com.universitymanagement.teacher.dto.response;

import com.universitymanagement.subject.dto.response.SubjectResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TeacherResponse(
        UUID teacherId,
        UUID userId,
        String keycloakId,
        String teacherCode,

        String fullName,
        String firstName,
        String lastName,
        String nameKhmer,
        String email,
        String phoneNumber,
        String gender,
        LocalDate dateOfBirth,
        String idCardNumber,
        String placeOfBirth,
        String currentAddress,
        String address,
        String avatarUrl,

        String specialization,
        List<TeacherDepartmentResponse> departments,
        List<UUID> departmentIds,
        String position,
        LocalDate hireDate,
        String employmentStatus,
        String status,
        List<SubjectResponse> subjects
) {
}