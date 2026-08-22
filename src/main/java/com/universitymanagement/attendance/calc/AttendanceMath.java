package com.universitymanagement.attendance.calc;

import com.universitymanagement.attendance.entity.AttendancePolicy;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.entity.AttendanceStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Turns marks into an attendance percentage under a classroom's policy.
 *
 * <p>One place decides what a late arrival is worth and what leaves the
 * denominator, so the register, the gradebook and the exam-eligibility check
 * can never disagree about a student's standing.
 */
@Component
public class AttendanceMath {

    /**
     * @param earned  credit the student accumulated
     * @param counted sessions that counted against them
     */
    public record Tally(double earned, double counted, int present, int late,
                        int absent, int excused, int unmarked) {

        public Double percent() {
            return counted > 0 ? Math.round(earned / counted * 10000.0) / 100.0 : null;
        }
    }

    /**
     * @param heldSessions how many sessions actually took place — the ceiling
     *                     for the denominator, so sessions the student was
     *                     never marked for are still visible as unmarked
     */
    public Tally tally(Collection<AttendanceRecord> records, int heldSessions,
                       AttendancePolicy policy) {
        double earned = 0.0;
        double counted = 0.0;
        int present = 0;
        int late = 0;
        int absent = 0;
        int excused = 0;

        for (AttendanceRecord record : records) {
            switch (effectiveStatus(record, policy)) {
                case PRESENT -> {
                    earned += 1.0;
                    counted += 1.0;
                    present++;
                }
                case LATE -> {
                    earned += policy.getLateCredit() != null ? policy.getLateCredit() : 0.5;
                    counted += 1.0;
                    late++;
                }
                case ABSENT -> {
                    counted += 1.0;
                    absent++;
                }
                case EXCUSED -> {
                    excused++;
                    // An excused absence normally leaves the denominator; a
                    // policy can instead make it count as a plain absence.
                    if (!Boolean.TRUE.equals(policy.getExcusedAbsencesIgnored())) {
                        counted += 1.0;
                    }
                }
            }
        }

        int marked = present + late + absent + excused;
        return new Tally(earned, counted, present, late, absent, excused,
                Math.max(heldSessions - marked, 0));
    }

    /**
     * A late arrival past the policy's threshold is recorded as late but
     * counted as an absence — which is how "more than 30 minutes and you may
     * as well not have come" is normally written down.
     */
    public AttendanceStatus effectiveStatus(AttendanceRecord record, AttendancePolicy policy) {
        AttendanceStatus status = record.getStatus();
        if (status != AttendanceStatus.LATE) {
            return status;
        }

        Integer threshold = policy.getLateBecomesAbsentAfterMinutes();
        Integer actual = record.getMinutesLate();

        return threshold != null && actual != null && actual > threshold
                ? AttendanceStatus.ABSENT
                : AttendanceStatus.LATE;
    }

    public boolean isEligibleForExam(Double percent, AttendancePolicy policy) {
        Double minimum = policy.getMinPercentToSitExam();
        if (minimum == null) {
            return true;
        }
        // Nothing recorded yet is not grounds for barring anyone.
        return percent == null || percent >= minimum;
    }
}
