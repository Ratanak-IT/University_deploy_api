package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.SessionStatus;
import com.universitymanagement.attendance.entity.SessionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** One meeting, with enough counts for the list to show progress at a glance. */
public record SessionResponse(
        UUID sessionId,
        UUID classroomId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        SessionType type,
        SessionStatus status,
        String topic,
        String cancellationReason,
        LocalDateTime takenAt,

        int rosterSize,
        int markedCount,
        int presentCount,
        int lateCount,
        int absentCount,
        int excusedCount
) {
    /** True once every enrolled student has a mark. */
    public boolean isComplete() {
        return rosterSize > 0 && markedCount >= rosterSize;
    }
}
