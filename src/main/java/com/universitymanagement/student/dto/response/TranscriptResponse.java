package com.universitymanagement.student.dto.response;

import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String programName,
        List<TermResponse> terms,
        Double cumulativeGpa,
        Double totalCredits
) {
    public record TermResponse(
            String academicYear,
            Integer semester,
            List<GradeResponse> grades,
            Double termGpa,
            Double termCredits
    ) {
    }
}
