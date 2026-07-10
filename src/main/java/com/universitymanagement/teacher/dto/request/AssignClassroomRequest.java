package com.universitymanagement.teacher.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignClassroomRequest(
        @NotNull(message = "Classroom ID is required")
        UUID classroomId
) {
}
