package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.AttendanceStatus;

import java.util.List;
import java.util.UUID;

/**
 * The mark sheet for one session: the whole roster, each student carrying
 * whatever mark they already have.
 *
 * <p>Returning the roster rather than only the saved rows is what lets the UI
 * show who is still unmarked — the old endpoint returned saved rows alone, so
 * an untouched student was indistinguishable from an absent one.
 */
public record SessionRegisterResponse(
        SessionResponse session,
        String className,
        String subjectName,
        List<StudentMark> students
) {
    public record StudentMark(
            UUID studentId,
            String studentCode,
            String fullName,
            String avatarUrl,
            /** Null when this student has not been marked yet. */
            AttendanceStatus status,
            Integer minutesLate,
            String remark,
            String excuseReference,
            /** Attendance across the whole course so far, for context while marking. */
            Double attendancePercent
    ) {
    }
}
