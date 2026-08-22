package com.universitymanagement.attendance;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The date walk that lays a weekly timetable out across a term.
 *
 * <p>Mirrors {@code ClassScheduleServiceImpl.generateSessions} without its
 * repositories, so the rule that matters — running it twice must never
 * duplicate a slot — is pinned down independently of the database.
 */
class SessionGenerationTest {

    private record Slot(DayOfWeek day, LocalTime start) {
    }

    private record Result(int created, int skippedExisting, int skippedHolidays) {
    }

    private Result generate(List<Slot> slots, LocalDate from, LocalDate to,
                            Set<LocalDate> holidays, Set<String> existing) {
        int created = 0;
        int skippedExisting = 0;
        int skippedHolidays = 0;

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (holidays.contains(date)) {
                skippedHolidays++;
                continue;
            }
            for (Slot slot : slots) {
                if (slot.day() != date.getDayOfWeek()) {
                    continue;
                }
                if (!existing.add(date + "|" + slot.start())) {
                    skippedExisting++;
                    continue;
                }
                created++;
            }
        }
        return new Result(created, skippedExisting, skippedHolidays);
    }

    private final List<Slot> twiceAWeek = List.of(
            new Slot(DayOfWeek.MONDAY, LocalTime.of(8, 0)),
            new Slot(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0)));

    @Test
    void laysEverySlotOutAcrossTheTerm() {
        // Mon 5 Jan 2026 through Sun 1 Feb 2026 — four Mondays, four Wednesdays.
        Result result = generate(twiceAWeek,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 1),
                Set.of(), new HashSet<>());

        assertEquals(8, result.created());
        assertEquals(0, result.skippedExisting());
    }

    @Test
    void runningItAgainCreatesNothing() {
        Set<String> calendar = new HashSet<>();
        LocalDate from = LocalDate.of(2026, 1, 5);
        LocalDate to = LocalDate.of(2026, 2, 1);

        Result first = generate(twiceAWeek, from, to, Set.of(), calendar);
        Result second = generate(twiceAWeek, from, to, Set.of(), calendar);

        assertEquals(8, first.created());
        assertEquals(0, second.created(),
                "re-running must not duplicate sessions or overwrite marks");
        assertEquals(8, second.skippedExisting());
    }

    @Test
    void extendingTheRangeOnlyFillsTheNewDates() {
        Set<String> calendar = new HashSet<>();
        LocalDate from = LocalDate.of(2026, 1, 5);

        generate(twiceAWeek, from, LocalDate.of(2026, 1, 18), Set.of(), calendar);
        Result extended = generate(twiceAWeek, from, LocalDate.of(2026, 2, 1), Set.of(), calendar);

        assertEquals(4, extended.created(), "only the two new weeks are added");
    }

    @Test
    void holidaysAreSkippedEntirely() {
        // 5 Jan and 7 Jan are the first Monday and Wednesday.
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 7));

        Result result = generate(twiceAWeek,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 1),
                holidays, new HashSet<>());

        assertEquals(6, result.created());
        assertEquals(2, result.skippedHolidays());
    }

    @Test
    void twoMeetingsOnOneDayBothGetASession() {
        List<Slot> twiceOnMonday = List.of(
                new Slot(DayOfWeek.MONDAY, LocalTime.of(8, 0)),
                new Slot(DayOfWeek.MONDAY, LocalTime.of(14, 0)));

        Result result = generate(twiceOnMonday,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5),
                Set.of(), new HashSet<>());

        assertEquals(2, result.created(),
                "a lecture and a lab on the same day are two separate registers");
    }

    @Test
    void aRangeWithNoMatchingWeekdayProducesNothing() {
        // Tue 6 Jan to Tue 6 Jan — neither a Monday nor a Wednesday.
        Result result = generate(twiceAWeek,
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 6),
                Set.of(), new HashSet<>());

        assertEquals(0, result.created());
        assertTrue(result.skippedHolidays() == 0);
    }
}
