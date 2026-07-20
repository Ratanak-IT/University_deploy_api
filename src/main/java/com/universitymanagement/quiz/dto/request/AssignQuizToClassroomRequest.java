package com.universitymanagement.quiz.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignQuizToClassroomRequest(
        @NotNull(message = "classroomId is required")
        UUID classroomId
) {
}
