package com.universitymanagement.teacher.dto.response;

/** Aggregate counts for the teacher dashboard — each field is one COUNT query, not a full entity fetch. */
public record TeacherDashboardSummaryResponse(
        long activeClasses,
        long totalStudents,
        long courseMaterials,
        long toGrade,
        long attendanceToday
) {
}
