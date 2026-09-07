package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class ClassroomFullException extends RuntimeException {
    public ClassroomFullException(UUID classroomId) {
        super("Classroom is at full capacity: " + classroomId);
    }
}
