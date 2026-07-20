package com.universitymanagement.subject.exception;

public class DuplicateSubjectException extends RuntimeException {
    public DuplicateSubjectException(String message) {
        super(message);
    }
}
