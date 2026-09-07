package com.universitymanagement.teacher.dto.response;

import java.util.UUID;

/**
 * Per-student attendance/performance, averaged only across this teacher's own
 * classrooms — lets the roster screen recompute its aggregate cards for
 * whatever subset the classroom/year/status filters currently show, instead
 * of one number for the whole teacher that never moves.
 */
public record StudentMetricsResponse(
        UUID studentId,
        Double attendancePercent,
        Double performancePercent
) {
}
