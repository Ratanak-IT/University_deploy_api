package com.universitymanagement.attendance.dto.request;

import com.universitymanagement.attendance.entity.SessionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Opens a meeting so the register has something to mark against. */
public record CreateSessionRequest(
        @NotNull(message = "sessionDate is required")
        LocalDate sessionDate,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        LocalTime endTime,

        SessionType type,

        @Size(max = 300, message = "topic must be at most 300 characters")
        String topic
) {
}
