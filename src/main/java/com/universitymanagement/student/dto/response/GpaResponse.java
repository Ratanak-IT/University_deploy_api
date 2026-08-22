package com.universitymanagement.student.dto.response;

import com.universitymanagement.grading.dto.response.CourseGradeResponse;

import java.util.List;
import java.util.UUID;

public record GpaResponse(
        UUID studentId,
        String studentCode,
        /** Official GPA — posted grades only. */
        Double cumulativeGpa,
        /** Includes courses still being marked. */
        Double currentGpa,
        Double creditsEarned,
        Double creditsAttempted,
        List<CourseGradeResponse> subjects
) {
}
