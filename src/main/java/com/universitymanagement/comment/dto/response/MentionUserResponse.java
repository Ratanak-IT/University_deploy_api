package com.universitymanagement.comment.dto.response;

import java.util.UUID;

public record MentionUserResponse(
        UUID userId,
        String fullName,
        String nameKhmer,
        String email,
        String role,
        String avatarUrl
) {
}
