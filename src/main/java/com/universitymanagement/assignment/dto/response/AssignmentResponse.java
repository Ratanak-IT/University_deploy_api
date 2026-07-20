package com.universitymanagement.assignment.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
        UUID assignmentId,
        UUID classroomId,
        String title,
        String description,
        LocalDateTime dueDate,
        Double maxScore,
        Double weight,
        List<FileResponse> files,
        LocalDateTime createdAt,
        String createdBy
) {
}
