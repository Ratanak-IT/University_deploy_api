package com.universitymanagement.quiz.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuizManageResponse(
        UUID quizId,
        UUID classroomId,
        String className,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer durationMinutes,
        Integer maxAttempts,
        String status,
        List<QuizQuestionManageResponse> questions
) {
    public record QuizQuestionManageResponse(
            UUID questionId,
            String questionText,
            List<String> options,
            String correctAnswer,
            Double score,
            Integer questionOrder
    ) {
    }
}
