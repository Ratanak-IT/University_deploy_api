package com.universitymanagement.attendance.dto.response;

import java.time.LocalDate;

/** What laying out the timetable actually did, so nothing happens silently. */
public record GenerateSessionsResponse(
        int created,
        /** Slots that already had a session and were left untouched. */
        int skippedExisting,
        int skippedHolidays,
        LocalDate from,
        LocalDate to
) {
}
