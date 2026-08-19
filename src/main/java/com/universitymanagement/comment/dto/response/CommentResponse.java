package com.universitymanagement.comment.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID commentId,
        UUID classroomId,
        UUID assignmentId,
        UUID parentId,

        String body,
        boolean edited,

        UUID authorUserId,
        String authorName,
        String authorRole,
        String authorAvatarUrl,

        List<MentionUserResponse> mentions,
        List<CommentResponse> replies,

        boolean canEdit,
        boolean canDelete,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}