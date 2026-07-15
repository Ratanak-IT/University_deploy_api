package com.universitymanagement.student.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/** Full student row for the admin list / detail views. */
public record StudentAdminResponse(
        UUID studentId,
        UUID userId,
        String keycloakId,
        String studentCode,
        String fullName,
        String email,
        String phoneNumber,
        String gender,
        LocalDate dateOfBirth,
        String academicYear,
        Integer yearLevel,
        Integer semester,
        String programName,
        LocalDate enrollmentDate,
        String graduationStatus,
        LocalDate graduationDate
) {
}
