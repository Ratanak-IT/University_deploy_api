package com.universitymanagement.grading.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Creates or updates one gradable column inside a component. */
public record AssessmentRequest(
        @NotNull(message = "componentId is required")
        UUID componentId,

        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotNull(message = "maxScore is required")
        @Positive(message = "maxScore must be greater than 0")
        Double maxScore,

        Integer position,

        LocalDateTime dueDate
) {
}
