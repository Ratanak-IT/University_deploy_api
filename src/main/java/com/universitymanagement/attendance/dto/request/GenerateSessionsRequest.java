package com.universitymanagement.attendance.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Lays the timetable out across a date range as scheduled sessions.
 *
 * <p>Existing sessions are left alone, so running this twice — or extending
 * the range later — never duplicates a slot or discards marks already taken.
 */
public record GenerateSessionsRequest(
        /** Defaults to the classroom's start date. */
        LocalDate from,

        /** Defaults to the classroom's end date. */
        LocalDate to,

        /** Dates with no class — public holidays, exam week. */
        List<LocalDate> skipDates
) {
}
