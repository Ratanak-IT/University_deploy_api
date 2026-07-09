package com.universitymanagement.assignment.dto.response;

import com.universitymanagement.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubmissionResponse(
        UUID submissionId,
        UUID assignmentId,
        UUID studentId,
        String studentCode,
        String studentName,
        String fileOriginalName,
        String fileUrl,
        LocalDateTime submittedAt,
        SubmissionStatus status,
        Double score,
        String feedback,
        LocalDateTime gradedAt
) {
}
