package com.universitymanagement.assignment.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** One message in a student's private thread with their teacher. */
public record PrivateCommentResponse(
        UUID commentId,
        UUID assignmentId,
        UUID studentId,

        UUID authorUserId,
        String authorName,
        /** "STUDENT" or "TEACHER" — the only two parties a private thread can have. */
        String authorRole,
        String authorAvatarUrl,

        String body,
        LocalDateTime createdAt
) {
}
