package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class TeacherAlreadyAssignedException extends RuntimeException {
    public TeacherAlreadyAssignedException(UUID teacherId, UUID classroomId) {
        super("Teacher " + teacherId + " is already assigned to classroom " + classroomId);
    }
}
