package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class StudentNotEnrolledInAssignmentException extends RuntimeException {
    public StudentNotEnrolledInAssignmentException(UUID classroomId) {
        super("You are not enrolled in classroom: " + classroomId);
    }
}
