package com.universitymanagement.quiz.dto.response;

import com.universitymanagement.quiz.entity.AttemptStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuizAttemptResponse(
        UUID attemptId,
        UUID quizId,
        String quizTitle,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt,
        Double earnedScore,
        Double totalScore,
        List<QuizQuestionResponse> questions,
        List<AnswerResult> answers
) {
    public record AnswerResult(
            UUID questionId,
            String answer,
            Boolean isCorrect,
            Double earnedScore
    ) {
    }
}
