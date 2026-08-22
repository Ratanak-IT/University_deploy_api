package com.universitymanagement.student.dto.response;

import com.universitymanagement.grading.entity.AcademicStanding;

import java.util.UUID;

/** One student's cumulative record — a row in the transcript cohort list. */
public record StudentAcademicSummaryResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String avatarUrl,

        UUID programId,
        String programName,
        Integer yearLevel,
        Integer semester,
        String academicYear,
        String status,

        /** Credits from passing, posted grades. */
        Double creditsEarned,
        Double creditsAttempted,
        /** The programme's requirement, when it is known. */
        Double creditsRequired,

        /** Official — posted grades only. Null before anything has been posted. */
        Double cumulativeGpa,
        /** Includes courses still being marked. An estimate. */
        Double currentGpa,

        AcademicStanding standing,
        String standingLabel,

        int coursesPosted,
        int coursesInProgress,
        boolean eligibleToGraduate
) {
}
