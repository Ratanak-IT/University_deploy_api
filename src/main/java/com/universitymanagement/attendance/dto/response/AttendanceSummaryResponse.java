package com.universitymanagement.attendance.dto.response;

import java.util.UUID;

/** A student's standing in one classroom's register. */
public record AttendanceSummaryResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        String avatarUrl,

        int sessionsHeld,
        int present,
        int late,
        int absent,
        int excused,
        /** Held sessions with no mark for this student. */
        int unmarked,

        /** Credit earned over sessions counted, as a percentage. */
        Double attendancePercent,
        /** False when the policy's minimum bars them from the final exam. */
        boolean eligibleForExam,
        Double minPercentToSitExam
) {
}
