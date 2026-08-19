package com.universitymanagement.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CommentUpdateRequest(

        @NotBlank(message = "Comment cannot be empty")
        @Size(max = 5000, message = "Comment must be 5000 characters or fewer")
        String body,

        List<UUID> mentionedUserIds
) {
}
