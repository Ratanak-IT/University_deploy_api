package com.universitymanagement.assignment.exception;

public class MissingSubmissionFileException extends RuntimeException {
    public MissingSubmissionFileException() {
        super("At least one submission file is required");
    }
}
