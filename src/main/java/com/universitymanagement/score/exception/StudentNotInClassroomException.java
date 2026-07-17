package com.universitymanagement.score.exception;

import java.util.UUID;

public class StudentNotInClassroomException extends RuntimeException {
    public StudentNotInClassroomException(UUID studentId, UUID classroomId) {
        super("Student " + studentId + " is not enrolled in classroom " + classroomId);
    }
}
