package com.universitymanagement.grading.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssessmentResponse(
        UUID assessmentId,
        UUID componentId,
        String title,
        Double maxScore,
        Integer position,
        LocalDateTime dueDate,
        /** Set when this column mirrors an assignment or quiz instead of manual entry. */
        String linkedTo,
        UUID linkedId,
        boolean editable
) {
}
