package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class TeacherNotInClassroomException extends RuntimeException {
    public TeacherNotInClassroomException(UUID teacherId, UUID classroomId) {
        super("Teacher " + teacherId + " is not assigned to classroom " + classroomId);
    }
}
