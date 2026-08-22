package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.AttendanceStatus;
import com.universitymanagement.attendance.entity.SessionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One mark, as a student sees it in their own history.
 *
 * <p>It carries the session's time as well as its date: a course that meets
 * twice on a Tuesday produces two of these, and without the time they would be
 * indistinguishable.
 */
public record AttendanceResponse(
        UUID recordId,
        UUID sessionId,
        UUID classroomId,
        String className,
        String subjectName,
        UUID studentId,
        String studentCode,
        String studentName,

        LocalDate attendanceDate,
        LocalTime startTime,
        LocalTime endTime,
        SessionType sessionType,
        String topic,

        AttendanceStatus status,
        Integer minutesLate,
        String remark,
        String excuseReference
) {
}
