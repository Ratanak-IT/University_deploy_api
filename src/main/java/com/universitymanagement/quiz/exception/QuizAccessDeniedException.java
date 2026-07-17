package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class QuizAccessDeniedException extends RuntimeException {
    public QuizAccessDeniedException(UUID quizId) {
        super("You do not have access to quiz: " + quizId);
    }

    public QuizAccessDeniedException(String message) {
        super(message);
    }
}
