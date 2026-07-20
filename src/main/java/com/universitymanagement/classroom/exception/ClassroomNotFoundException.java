package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class ClassroomNotFoundException extends RuntimeException {
    public ClassroomNotFoundException(UUID classroomId) {
        super("Classroom not found with id: " + classroomId);
    }

    public ClassroomNotFoundException(String message) {
        super(message);
    }
}
