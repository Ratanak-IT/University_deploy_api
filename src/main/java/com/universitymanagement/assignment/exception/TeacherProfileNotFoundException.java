package com.universitymanagement.assignment.exception;

public class TeacherProfileNotFoundException extends RuntimeException {
    public TeacherProfileNotFoundException() {
        super("Teacher profile not found for current user");
    }
}
