package com.universitymanagement.attendance.dto.response;

import com.universitymanagement.attendance.entity.SessionType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One weekly slot, as a student sees it across all their enrolled
 * classrooms — {@link ScheduleSlotResponse} is already scoped to a single
 * classroom, so it has no classroom or subject fields; a student's combined
 * timetable needs both to tell classes apart on the same day.
 */
public record StudentTimetableSlotResponse(
        UUID scheduleId,
        UUID classroomId,
        String className,
        String classCode,
        String subjectCode,
        String subjectName,
        String teacherName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        SessionType type,
        String room
) {
}
