package com.universitymanagement.teacher.exception;

import java.util.UUID;

public class TeacherInactiveException extends RuntimeException {
    public TeacherInactiveException(UUID teacherId) {
        super("Teacher " + teacherId + " is not active and cannot be assigned to a classroom");
    }
}
