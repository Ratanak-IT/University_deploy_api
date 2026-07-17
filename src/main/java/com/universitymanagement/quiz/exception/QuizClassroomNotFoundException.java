package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class QuizClassroomNotFoundException extends RuntimeException {
    public QuizClassroomNotFoundException(UUID classroomId) {
        super("Classroom not found with id: " + classroomId);
    }
}
