package com.universitymanagement.grading.entity;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The institution's official grading scale — one place that owns the
 * percent -> letter -> grade point mapping, so the three can never drift apart.
 *
 * <p>Letters with a {@code minPercent} are awarded by calculation. The rest
 * ({@link #I}, {@link #W}, {@link #P}, {@link #NP}) are administrative marks a
 * registrar assigns by hand.
 */
public enum LetterGrade {

    A("A", 4.0, 90.0, true, true),
    A_MINUS("A-", 3.7, 85.0, true, true),
    B_PLUS("B+", 3.3, 80.0, true, true),
    B("B", 3.0, 75.0, true, true),
    B_MINUS("B-", 2.7, 70.0, true, true),
    C_PLUS("C+", 2.3, 65.0, true, true),
    C("C", 2.0, 60.0, true, true),
    D_PLUS("D+", 1.7, 55.0, true, true),
    D("D", 1.0, 50.0, true, true),
    F("F", 0.0, 0.0, false, true),

    /** Incomplete — coursework outstanding, no point value until resolved. */
    I("I", null, null, false, false),
    /** Withdrawn — appears on the transcript but never affects GPA. */
    W("W", null, null, false, false),
    /** Passed on a pass/no-pass basis: earns credit, excluded from GPA. */
    P("P", null, null, true, false),
    /** Not passed on a pass/no-pass basis. */
    NP("NP", null, null, false, false);

    private final String display;
    private final Double gradePoint;
    private final Double minPercent;
    private final boolean passing;
    private final boolean countsInGpa;

    LetterGrade(String display, Double gradePoint, Double minPercent,
                boolean passing, boolean countsInGpa) {
        this.display = display;
        this.gradePoint = gradePoint;
        this.minPercent = minPercent;
        this.passing = passing;
        this.countsInGpa = countsInGpa;
    }

    public String getDisplay() {
        return display;
    }

    public Double getGradePoint() {
        return gradePoint;
    }

    public Double getMinPercent() {
        return minPercent;
    }

    public boolean isPassing() {
        return passing;
    }

    public boolean isCountsInGpa() {
        return countsInGpa;
    }

    /** True for letters that a percentage can produce; false for I/W/P/NP. */
    public boolean isCalculated() {
        return minPercent != null;
    }

    public static LetterGrade fromPercent(double percent) {
        return Arrays.stream(values())
                .filter(LetterGrade::isCalculated)
                .sorted(Comparator.comparingDouble(LetterGrade::getMinPercent).reversed())
                .filter(g -> percent >= g.minPercent)
                .findFirst()
                .orElse(F);
    }
}
