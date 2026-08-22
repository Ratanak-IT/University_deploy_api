package com.universitymanagement.attendance.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * A student's attendance, per course.
 *
 * <p>A flat list of marks answers "what happened on the 14th"; what a student
 * actually needs to know is whether they are still allowed to sit the exam,
 * which only makes sense per course.
 */
public record StudentAttendanceResponse(
        UUID classroomId,
        String className,
        String subjectCode,
        String subjectName,
        String academicYear,
        Integer semester,

        int sessionsHeld,
        int present,
        int late,
        int absent,
        int excused,
        int unmarked,

        Double attendancePercent,
        boolean eligibleForExam,
        Double minPercentToSitExam,

        List<AttendanceResponse> records
) {
}
