package com.universitymanagement.student.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record StudentAdminResponse(
        UUID studentId,
        UUID userId,
        String keycloakId,
        String studentCode,

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
        String fatherContact,
        String motherContact,
        String avatarUrl,

        String academicYear,
        Integer yearLevel,
        Integer semester,
        UUID programId,
        String programName,
        UUID departmentId,
        String departmentName,
        Double gpa,
        LocalDate enrollmentDate,
        String status,
        String graduationStatus,
        LocalDate graduationDate
) {
}