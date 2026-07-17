package com.universitymanagement.quiz.exception;

import com.universitymanagement.quiz.entity.AttemptStatus;

import java.util.UUID;

public class QuizAttemptAlreadyFinalizedException extends RuntimeException {
    public QuizAttemptAlreadyFinalizedException(UUID attemptId, AttemptStatus status) {
        super("Attempt " + attemptId + " is already " + status);
    }
}
