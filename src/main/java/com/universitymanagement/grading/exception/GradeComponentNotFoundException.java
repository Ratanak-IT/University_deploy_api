package com.universitymanagement.grading.exception;

import java.util.UUID;

public class GradeComponentNotFoundException extends RuntimeException {
    public GradeComponentNotFoundException(UUID componentId) {
        super("Grade component not found with id: " + componentId);
    }
}
