package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class ScheduleConflictException extends RuntimeException {
    public ScheduleConflictException(UUID studentId, UUID classroomId) {
        super("Student " + studentId + " has a schedule conflict with classroom " + classroomId);
    }
}
