package com.universitymanagement.student.dto.response;

import java.util.UUID;

public record StudentResponse(
        UUID id,
        String studentCode,
        String email,
        String department
) {
}
