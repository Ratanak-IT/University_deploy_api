package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class SubmissionAlreadyGradedException extends RuntimeException {
    public SubmissionAlreadyGradedException(UUID submissionId) {
        super("Submission " + submissionId + " has already been graded");
    }
}
