package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class ClassroomMemberAccessDeniedException extends RuntimeException {
    public ClassroomMemberAccessDeniedException(UUID classroomId) {
        super("You are not a member of classroom: " + classroomId);
    }
}
