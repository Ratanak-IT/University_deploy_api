package com.universitymanagement.assignment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AssignmentRequest(
        @NotBlank(message = "Assignment title is required")
        @Size(max = 200)
        String title,

        String description,

        @NotNull(message = "Due date is required")
        @Future(message = "Due date must be in the future")
        LocalDateTime dueDate,
        //file and video

        Double maxScore,

        Double weight
) {
}
