package com.universitymanagement.assignment.dto.response;

import com.universitymanagement.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        /** Null for a MISSING row: there is no submission to identify. */
        UUID submissionId,
        UUID assignmentId,
        UUID studentId,
        String studentCode,
        String studentName,
        /** Presigned MinIO URL for the student's avatar. Null when unset. */
        String avatarUrl,
        List<FileResponse> files,
        LocalDateTime submittedAt,
        SubmissionStatus status,
        Double score,
        String feedback,
        LocalDateTime gradedAt
) {
}
