package com.universitymanagement.attendance.entity;

/**
 * Whether a scheduled meeting actually took place.
 *
 * <p>This is what makes an attendance percentage trustworthy: only
 * {@link #HELD} sessions land in the denominator, so a public holiday or a
 * cancelled class never counts against a student.
 */
public enum SessionStatus {

    /** On the timetable, not yet taught. Excluded until it is held. */
    SCHEDULED,

    /** Taught and marked. */
    HELD,

    /** Called off — holiday, teacher absent, campus closed. Never counted. */
    CANCELLED
}
