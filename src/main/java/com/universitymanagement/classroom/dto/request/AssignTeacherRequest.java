package com.universitymanagement.classroom.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTeacherRequest(
        @NotNull(message = "Teacher ID is required")
        UUID teacherId
) {
}
