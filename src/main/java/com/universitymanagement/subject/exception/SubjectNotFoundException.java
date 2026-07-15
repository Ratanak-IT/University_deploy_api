package com.universitymanagement.subject.exception;

import java.util.UUID;

public class SubjectNotFoundException extends RuntimeException {
    public SubjectNotFoundException(UUID subjectId) {
        super("Subject not found with id: " + subjectId);
    }

    public SubjectNotFoundException(String message) {
        super(message);
    }
}
