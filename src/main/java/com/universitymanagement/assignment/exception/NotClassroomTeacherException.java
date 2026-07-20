package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class NotClassroomTeacherException extends RuntimeException {
    public NotClassroomTeacherException(UUID classroomId) {
        super("You are not the teacher of classroom: " + classroomId);
    }
}
