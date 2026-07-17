package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(UUID assignmentId) {
        super("Assignment not found with id: " + assignmentId);
    }

    public AssignmentNotFoundException(String message) {
        super(message);
    }
}
