package com.universitymanagement.classroom.exception;

public class ClassroomAccessDeniedException extends RuntimeException {
    public ClassroomAccessDeniedException(String message) {
        super(message);
    }
}
