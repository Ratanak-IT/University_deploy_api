package com.universitymanagement.quiz.exception;

import java.util.UUID;

public class StudentNotEnrolledInQuizException extends RuntimeException {
    public StudentNotEnrolledInQuizException(UUID studentId, UUID quizId) {
        super("Student " + studentId + " is not enrolled in the classroom of quiz " + quizId);
    }

    public StudentNotEnrolledInQuizException(String message) {
        super(message);
    }
}
