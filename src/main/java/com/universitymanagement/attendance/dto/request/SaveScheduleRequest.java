package com.universitymanagement.attendance.dto.request;

import com.universitymanagement.attendance.entity.SessionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * The classroom's weekly timetable. The list replaces what is there, since a
 * timetable is only meaningful as a whole.
 */
public record SaveScheduleRequest(
        @NotNull(message = "slots are required")
        List<@Valid Slot> slots
) {
    public record Slot(
            @NotNull(message = "dayOfWeek is required")
            DayOfWeek dayOfWeek,

            @NotNull(message = "startTime is required")
            LocalTime startTime,

            LocalTime endTime,

            SessionType type,

            @Size(max = 50, message = "room must be at most 50 characters")
            String room
    ) {
    }
}
