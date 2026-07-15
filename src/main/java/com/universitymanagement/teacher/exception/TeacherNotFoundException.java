package com.universitymanagement.teacher.exception;

import java.util.UUID;

public class TeacherNotFoundException extends RuntimeException {
    public TeacherNotFoundException(UUID teacherId) {
        super("Teacher not found with id: " + teacherId);
    }

    public TeacherNotFoundException(String message) {
        super(message);
    }}
