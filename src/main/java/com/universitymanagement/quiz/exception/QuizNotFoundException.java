package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class QuizNotFoundException extends RuntimeException {
    public QuizNotFoundException(UUID quizId) {
        super("Quiz not found with id: " + quizId);
    }

    public QuizNotFoundException(String message) {
        super(message);
    }
}
