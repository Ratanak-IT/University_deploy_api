package com.universitymanagement.quiz.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuizManageResponse(
        UUID quizId,

        /**
         * The first classroom the quiz reached, kept so older clients still
         * render something. Legacy — read {@code classrooms} for the full set.
         */
        UUID classroomId,
        String className,

        /** Every classroom this quiz has been released to. */
        List<QuizClassroomResponse> classrooms,

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
