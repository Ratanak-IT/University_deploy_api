package com.universitymanagement.grading.dto.response;

import com.universitymanagement.grading.entity.ComponentSource;

import java.util.List;
import java.util.UUID;

public record GradeComponentResponse(
        UUID componentId,
        String name,
        ComponentSource source,
        Double weightPercent,
        Integer position,
        /** False for derived components, whose columns the teacher cannot edit. */
        boolean editable,
        List<AssessmentResponse> assessments
) {
}
