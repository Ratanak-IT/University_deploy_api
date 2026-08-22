package com.universitymanagement.quiz.entity;

/**
 * What kind of answer a question expects.
 *
 * <p>Before this existed every question was implicitly a single-answer multiple
 * choice, and the client declared three types it had no way to store.
 */
public enum QuestionType {

    /** One correct choice out of the supplied options. */
    MULTIPLE_CHOICE,

    /**
     * A two-option special case of {@link #MULTIPLE_CHOICE}.
     *
     * <p>Graded identically — it exists so the UI can render a pair of buttons
     * rather than a list, and so a report can tell the two apart.
     */
    TRUE_FALSE,

    /**
     * Free text, compared case-insensitively after trimming.
     *
     * <p>There is deliberately no fuzzy matching: a near-miss must not be
     * silently marked correct, and a teacher can review these afterwards.
     */
    SHORT_ANSWER
}
