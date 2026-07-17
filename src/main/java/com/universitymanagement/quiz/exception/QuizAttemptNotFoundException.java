package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class QuizAttemptNotFoundException extends RuntimeException {
    public QuizAttemptNotFoundException(UUID attemptId) {
        super("Quiz attempt not found with id: " + attemptId);
    }

    public QuizAttemptNotFoundException(String message) {
        super(message);
    }
}
