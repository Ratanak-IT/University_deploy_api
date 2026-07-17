package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class ClassroomHasNoTeacherException extends RuntimeException {
    public ClassroomHasNoTeacherException(UUID classroomId) {
        super("Classroom " + classroomId + " has no teacher assigned");
    }
}
