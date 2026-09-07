package com.universitymanagement.teacher.dto.response;

/** Aggregate counts for the teacher dashboard — each field is one COUNT query, not a full entity fetch. */
public record TeacherDashboardSummaryResponse(
        long activeClasses,
        long totalStudents,
        long courseMaterials,
        long toGrade,
        long attendanceToday,
        /** Average attendance credit across every student in every one of this teacher's classrooms. Null when there's nothing to average yet. */
        Double avgAttendancePercent,
        /** Average posted/in-progress score across every student in every one of this teacher's classrooms. Null when nothing is graded yet. */
        Double avgPerformancePercent
) {
}
