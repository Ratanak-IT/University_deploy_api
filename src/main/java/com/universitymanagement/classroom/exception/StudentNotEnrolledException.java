package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class StudentNotEnrolledException extends RuntimeException {
    public StudentNotEnrolledException(UUID studentId) {
        super("Student not found with id: " + studentId);

    }
    public StudentNotEnrolledException(String message) {
        super(message);
    }
}
