package com.universitymanagement.classroom.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

// Student inside a classroom (for teacher/admin "view students in classroom")
public record ClassroomStudentResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String email,
        Integer yearLevel,
        Integer semester,
        LocalDateTime joinedAt
) {
}