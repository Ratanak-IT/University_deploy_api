package com.universitymanagement.grading.entity;

/** Lifecycle of a student's final grade for one course offering. */
public enum CourseGradeStatus {

    /** Still being marked. Recomputed on every score change; not transcript-worthy. */
    IN_PROGRESS,

    /** Teacher has signed off. Frozen against score edits, awaiting registrar. */
    SUBMITTED,

    /** Registrar posted it. Immutable, and the only status a transcript reads. */
    POSTED
}
