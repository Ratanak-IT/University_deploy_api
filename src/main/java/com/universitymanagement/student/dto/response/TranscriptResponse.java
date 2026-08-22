package com.universitymanagement.student.dto.response;

import com.universitymanagement.grading.dto.response.CourseGradeResponse;

import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String programName,
        List<TermResponse> terms,

        /** Official GPA — posted grades only. Null before anything has been posted. */
        Double cumulativeGpa,
        /** Includes courses still being marked. An estimate, never the record. */
        Double currentGpa,
        Double creditsEarned,
        Double creditsAttempted
) {
    public record TermResponse(
            String academicYear,
            Integer semester,
            List<CourseGradeResponse> grades,
            Double termGpa,
            Double termCredits
    ) {
    }
}
