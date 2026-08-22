package com.universitymanagement.grading.exception;

import com.universitymanagement.grading.entity.CourseGradeStatus;

import java.util.UUID;

/** Raised when a score edit would alter a grade that has already been signed off. */
public class GradeLockedException extends RuntimeException {
    public GradeLockedException(UUID classroomId, CourseGradeStatus status) {
        super("Grades for classroom " + classroomId + " are " + status
                + " and can no longer be edited. Ask the registrar to reopen them.");
    }
}
