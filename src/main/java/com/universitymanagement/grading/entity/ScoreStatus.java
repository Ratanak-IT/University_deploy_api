package com.universitymanagement.grading.entity;

/**
 * Why a score cell holds the value it does. The distinction matters: an
 * {@link #EXCUSED} student is removed from the denominator, a {@link #MISSING}
 * one scores zero, and a cell with no row at all is simply not graded yet.
 */
public enum ScoreStatus {

    /** A real mark was recorded. */
    GRADED,

    /** Student did not submit and the deadline passed — counts as zero. */
    MISSING,

    /** Formally excused — dropped from the calculation entirely. */
    EXCUSED
}
