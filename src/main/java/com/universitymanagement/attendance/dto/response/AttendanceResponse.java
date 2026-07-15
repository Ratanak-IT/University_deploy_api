package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID attendanceId,
        UUID classroomId,
        String className,
        String subjectName,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String remark
) {
}
