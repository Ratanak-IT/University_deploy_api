package com.universitymanagement.grading.entity;

/**
 * Where a student sits against the institution's progression rules.
 *
 * <p>This is the reason a registrar opens a cohort list at all — not to read
 * GPAs one by one, but to find the students who need attention before the term
 * ends. The thresholds are the common 4.0-scale ones; a school that uses
 * different ones changes them here, in the open, rather than in a report query.
 */
public enum AcademicStanding {

    /** Distinction — typically eligible for the dean's list. */
    DEANS_LIST("Dean's List", 3.5),

    /** Meeting the minimum to progress. */
    GOOD_STANDING("Good standing", 2.0),

    /** Below the minimum. Usually carries conditions for the next term. */
    PROBATION("Academic probation", 0.0),

    /** Nothing posted yet — a first-term student, not a failing one. */
    NO_RECORD("No record yet", null);

    private final String label;
    private final Double minGpa;

    AcademicStanding(String label, Double minGpa) {
        this.label = label;
        this.minGpa = minGpa;
    }

    public String getLabel() {
        return label;
    }

    public Double getMinGpa() {
        return minGpa;
    }

    /**
     * @param cumulativeGpa the official figure — posted grades only. A student
     *                      is never placed on probation on the strength of
     *                      marks that have not been signed off.
     */
    public static AcademicStanding of(Double cumulativeGpa) {
        if (cumulativeGpa == null) {
            return NO_RECORD;
        }
        if (cumulativeGpa >= DEANS_LIST.minGpa) {
            return DEANS_LIST;
        }
        if (cumulativeGpa >= GOOD_STANDING.minGpa) {
            return GOOD_STANDING;
        }
        return PROBATION;
    }
}
