package com.universitymanagement.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePrivateCommentRequest(
        @NotBlank(message = "Comment cannot be empty")
        @Size(max = 4000, message = "Comment must be at most 4000 characters")
        String body
) {
}
