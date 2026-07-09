package com.universitymanagement.assignment.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponse(
        UUID assignmentId,
        UUID classroomId,
        String title,
        String description,
        LocalDateTime dueDate,
        Double maxScore,
        Double weight,
        String fileOriginalName,
        String fileUrl,
        LocalDateTime createdAt,
        String createdBy
) {
}
