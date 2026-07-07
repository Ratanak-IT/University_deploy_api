package com.universitymanagement.student.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StudentDetailResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        List<String> roles,
        String studentCode,
        String academicYear,
        Integer yearLevel,
        Integer semester,
        LocalDate dob,
        String gender,
        String graduationStatus
) {
}
