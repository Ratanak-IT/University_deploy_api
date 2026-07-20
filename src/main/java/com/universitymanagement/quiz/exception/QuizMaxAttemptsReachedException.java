package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class QuizMaxAttemptsReachedException extends RuntimeException {
    public QuizMaxAttemptsReachedException(UUID quizId, int maxAttempts) {
        super("Maximum attempts reached (" + maxAttempts + ") for quiz " + quizId);
    }
}
