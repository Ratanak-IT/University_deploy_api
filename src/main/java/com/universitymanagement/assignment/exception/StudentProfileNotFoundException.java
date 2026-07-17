package com.universitymanagement.assignment.exception;

public class StudentProfileNotFoundException extends RuntimeException {
    public StudentProfileNotFoundException() {
        super("Student profile not found for current user");
    }
}
