package com.universitymanagement.quiz.dto.response;

import com.universitymanagement.quiz.entity.AttemptStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** One student's attempt at a quiz, for the teacher's results list — no question/answer detail. */
public record QuizAttemptSummaryResponse(
        UUID attemptId,
        UUID studentId,
        String studentCode,
        String studentName,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        Double earnedScore,
        Double totalScore
) {
}
