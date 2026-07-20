package com.universitymanagement.quiz.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuizResponse(
        UUID quizId,
        UUID classroomId,
        String className,
        String subjectName,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer durationMinutes,
        Integer maxAttempts,
        long attemptsUsed,
        Double bestScore
) {
}
