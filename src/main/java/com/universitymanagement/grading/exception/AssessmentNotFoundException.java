package com.universitymanagement.grading.exception;

import java.util.UUID;

public class AssessmentNotFoundException extends RuntimeException {
    public AssessmentNotFoundException(UUID assessmentId) {
        super("Assessment not found with id: " + assessmentId);
    }
}
