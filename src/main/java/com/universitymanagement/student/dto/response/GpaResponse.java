package com.universitymanagement.student.dto.response;

import java.util.List;
import java.util.UUID;

public record GpaResponse(
        UUID studentId,
        String studentCode,
        Double cumulativeGpa,
        Double totalCredits,
        List<GradeResponse> subjects
) {
}
