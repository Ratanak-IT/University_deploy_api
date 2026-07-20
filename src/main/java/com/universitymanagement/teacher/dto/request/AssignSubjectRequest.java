package com.universitymanagement.teacher.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignSubjectRequest(
        @NotNull(message = "Subject ID is required")
        UUID subjectId
) {
}
