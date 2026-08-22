package com.universitymanagement.grading.calc;

/**
 * A student's attendance in one classroom, already reduced to credit.
 *
 * <p>The policy — what a late arrival is worth, whether an excused absence
 * leaves the denominator — is applied by the attendance module before the
 * numbers get here. The grade calculator only needs the ratio, and keeping
 * the rules in one place is what stops the register and the gradebook
 * disagreeing about the same student.
 *
 * @param earned  credit accumulated across counted sessions
 * @param counted sessions that counted against the student
 */
public record AttendanceTally(double earned, double counted) {

    public double possible() {
        return counted;
    }

    public boolean hasData() {
        return counted > 0;
    }

    public static AttendanceTally empty() {
        return new AttendanceTally(0, 0);
    }
}
