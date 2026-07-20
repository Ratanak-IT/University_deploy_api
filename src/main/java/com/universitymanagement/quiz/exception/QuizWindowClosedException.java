package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class QuizWindowClosedException extends RuntimeException {
    public QuizWindowClosedException(UUID quizId, String reason) {
        super("Quiz " + quizId + " is not open for attempts: " + reason);
    }
}
