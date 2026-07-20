package com.universitymanagement.assignment.exception;

import java.util.UUID;

public class AssignmentClassroomNotFoundException extends RuntimeException {
    public AssignmentClassroomNotFoundException(UUID classroomId) {
        super("Classroom not found with id: " + classroomId);
    }
}
