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
        String createdBy,
        /** How many students have submitted. Null where the caller didn't ask for it (e.g. create/update). */
        Long submittedCount,
        /** Classroom roster size, for rendering "submitted / total". Null where not requested. */
        Long totalStudents
) {
}
