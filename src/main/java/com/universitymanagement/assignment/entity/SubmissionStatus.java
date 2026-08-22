package com.universitymanagement.assignment.entity;

public enum SubmissionStatus {
    SUBMITTED,
    LATE,
    GRADED,

    /**
     * On the roster but nothing handed in.
     *
     * <p>Never stored — no Submission row exists for these students. It is
     * produced when listing an assignment's submissions so a teacher can see
     * who is missing work, which the submissions table alone cannot show.
     */
    MISSING
}
