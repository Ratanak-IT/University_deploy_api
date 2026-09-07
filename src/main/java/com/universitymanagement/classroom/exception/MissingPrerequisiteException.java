package com.universitymanagement.classroom.exception;

import java.util.UUID;

/**
 * Enrolment refused because a required earlier subject has not been passed.
 *
 * <p>The message names the student and the subject. It used to carry two UUIDs,
 * which told the registrar that something was wrong and nothing about what to
 * do next — they still had to open the curriculum, find the entry, and look up
 * an id by hand before they could act.
 */
public class MissingPrerequisiteException extends RuntimeException {

    public MissingPrerequisiteException(String studentName, String prerequisiteLabel) {
        super(studentName + " has not passed " + prerequisiteLabel
                + " yet, which this course requires. Post a passing grade for it "
                + "first, or remove the prerequisite from the curriculum.");
    }

    /** Fallback for a prerequisite whose subject row no longer exists. */
    public MissingPrerequisiteException(String studentName, UUID prerequisiteSubjectId) {
        super(studentName + " cannot be enrolled: this course requires a prerequisite "
                + "subject (" + prerequisiteSubjectId + ") that no longer exists. "
                + "Fix the prerequisite on the curriculum entry.");
    }
}
