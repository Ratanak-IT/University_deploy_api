package com.universitymanagement.assignment.dto.response;

import com.universitymanagement.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID submissionId,
        UUID assignmentId,
        UUID studentId,
        String studentCode,
        String studentName,
        List<FileResponse> files,
        LocalDateTime submittedAt,
        SubmissionStatus status,
        Double score,
        String feedback,
        LocalDateTime gradedAt
) {
}
