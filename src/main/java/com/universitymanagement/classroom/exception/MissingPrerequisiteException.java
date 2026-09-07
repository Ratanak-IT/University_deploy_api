package com.universitymanagement.classroom.exception;

import java.util.UUID;

public class MissingPrerequisiteException extends RuntimeException {
    public MissingPrerequisiteException(UUID studentId, UUID prerequisiteSubjectId) {
        super("Student " + studentId + " has not passed the prerequisite subject " + prerequisiteSubjectId);
    }
}
