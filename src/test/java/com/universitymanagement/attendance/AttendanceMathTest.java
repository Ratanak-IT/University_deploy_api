package com.universitymanagement.attendance;

import com.universitymanagement.attendance.calc.AttendanceMath;
import com.universitymanagement.attendance.entity.AttendancePolicy;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.entity.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceMathTest {

    private final AttendanceMath math = new AttendanceMath();

    private AttendancePolicy policy() {
        return new AttendancePolicy();
    }

    private AttendanceRecord mark(AttendanceStatus status) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStatus(status);
        return record;
    }

    private AttendanceRecord late(int minutes) {
        AttendanceRecord record = mark(AttendanceStatus.LATE);
        record.setMinutesLate(minutes);
        return record;
    }

    @Test
    void lateEarnsThePolicysPartialCredit() {
        List<AttendanceRecord> records = List.of(
                mark(AttendanceStatus.PRESENT),
                mark(AttendanceStatus.PRESENT),
                late(5),
                mark(AttendanceStatus.ABSENT));

        AttendanceMath.Tally tally = math.tally(records, 4, policy());

        // 1 + 1 + 0.5 + 0 over four counted sessions.
        assertEquals(62.5, tally.percent());
    }

    @Test
    void excusedLeavesTheDenominatorEntirely() {
        List<AttendanceRecord> records = List.of(
                mark(AttendanceStatus.PRESENT),
                mark(AttendanceStatus.EXCUSED),
                mark(AttendanceStatus.EXCUSED));

        AttendanceMath.Tally tally = math.tally(records, 3, policy());

        assertEquals(100.0, tally.percent(),
                "an excused absence must not be scored as a miss");
        assertEquals(1.0, tally.counted());
    }

    @Test
    void aPolicyCanMakeExcusedAbsencesCount() {
        AttendancePolicy strict = policy();
        strict.setExcusedAbsencesIgnored(false);

        List<AttendanceRecord> records = List.of(
                mark(AttendanceStatus.PRESENT),
                mark(AttendanceStatus.EXCUSED));

        assertEquals(50.0, math.tally(records, 2, strict).percent());
    }

    @Test
    void arrivingTooLateIsCountedAsAnAbsence() {
        AttendancePolicy p = policy();
        p.setLateBecomesAbsentAfterMinutes(30);

        assertEquals(AttendanceStatus.LATE, math.effectiveStatus(late(20), p));
        assertEquals(AttendanceStatus.ABSENT, math.effectiveStatus(late(45), p));

        // 45 minutes late scores nothing, unlike the half credit for 20.
        assertEquals(0.0, math.tally(List.of(late(45)), 1, p).percent());
        assertEquals(50.0, math.tally(List.of(late(20)), 1, p).percent());
    }

    @Test
    void sessionsHeldButNotMarkedAreReportedRatherThanCountedAgainst() {
        AttendanceMath.Tally tally =
                math.tally(List.of(mark(AttendanceStatus.PRESENT)), 5, policy());

        assertEquals(100.0, tally.percent(),
                "unmarked sessions must not silently become absences");
        assertEquals(4, tally.unmarked());
    }

    @Test
    void noMarksAtAllGivesNoPercentage() {
        AttendanceMath.Tally tally = math.tally(new ArrayList<>(), 0, policy());

        assertNull(tally.percent(),
                "a class that has not met yet has no attendance figure, not zero");
    }

    @Test
    void eligibilityFollowsThePolicyMinimum() {
        AttendancePolicy p = policy();
        p.setMinPercentToSitExam(80.0);

        assertTrue(math.isEligibleForExam(85.0, p));
        assertTrue(math.isEligibleForExam(80.0, p), "the minimum itself passes");
        assertFalse(math.isEligibleForExam(79.9, p));

        assertTrue(math.isEligibleForExam(null, p),
                "a student with no record yet cannot be barred");
    }

    @Test
    void aPolicyWithoutAMinimumBarsNobody() {
        AttendancePolicy p = policy();
        p.setMinPercentToSitExam(null);

        assertTrue(math.isEligibleForExam(10.0, p));
    }
}
