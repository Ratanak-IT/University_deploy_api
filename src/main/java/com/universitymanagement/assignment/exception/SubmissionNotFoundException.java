package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class SubmissionNotFoundException extends RuntimeException {
    public SubmissionNotFoundException(UUID submissionId) {
        super("Submission not found with id: " + submissionId);
    }
}
