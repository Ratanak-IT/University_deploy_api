package com.universitymanagement.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record GradeSubmissionRequest(
        @NotNull(message = "Score is required")
        @PositiveOrZero(message = "Score cannot be negative")
        Double score,

        String feedback
) {
}
