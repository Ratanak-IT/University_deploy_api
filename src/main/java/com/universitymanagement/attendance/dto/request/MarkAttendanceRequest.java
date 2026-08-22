package com.universitymanagement.attendance.dto.request;

import com.universitymanagement.attendance.entity.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

/**
 * Marks a session's register.
 *
 * <p>`status` is required per student. The old endpoint parsed the body by hand
 * and fell back to PRESENT whenever it could not read a status, which quietly
 * credited attendance nobody had recorded — and, now that attendance carries
 * grade weight, quietly changed marks.
 */
public record MarkAttendanceRequest(
        @NotEmpty(message = "at least one student is required")
        List<@Valid StudentMark> marks,

        /** Closes the register, so the session starts counting. Defaults to true. */
        Boolean markSessionHeld
) {
    public record StudentMark(
            @NotNull(message = "studentId is required")
            UUID studentId,

            @NotNull(message = "status is required")
            AttendanceStatus status,

            @PositiveOrZero(message = "minutesLate cannot be negative")
            Integer minutesLate,

            String remark,

            String excuseReference
    ) {
    }

    public boolean shouldMarkHeld() {
        return markSessionHeld == null || markSessionHeld;
    }
}
