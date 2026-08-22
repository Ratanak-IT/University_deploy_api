package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.SessionType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleSlotResponse(
        UUID scheduleId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        SessionType type,
        String room
) {
}
