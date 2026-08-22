package com.universitymanagement.grading.entity;

/** Where the scores inside a {@link GradeComponent} come from. */
public enum ComponentSource {

    /** Teacher types the scores in the gradebook (exams, presentations, labs). */
    MANUAL,

    /** Derived from graded assignment submissions in this classroom. */
    ASSIGNMENT,

    /** Derived from the student's best quiz attempts in this classroom. */
    QUIZ,

    /** Derived from the attendance register — never typed by hand. */
    ATTENDANCE
}
