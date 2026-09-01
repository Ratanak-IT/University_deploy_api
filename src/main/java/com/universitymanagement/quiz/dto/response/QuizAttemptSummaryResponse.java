package com.universitymanagement.quiz.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One roster row for the teacher's per-quiz results table — every student
 * enrolled in a classroom the quiz was released to, whether they've
 * attempted it or not. {@code status} is one of the real
 * {@code AttemptStatus} values ({@code IN_PROGRESS}, {@code SUBMITTED},
 * {@code EXPIRED}) plus {@code NOT_STARTED}, which has no backing attempt
 * row — hence a plain String here rather than the enum itself.
 */
public record QuizAttemptSummaryResponse(
        UUID attemptId,
        UUID studentId,
        String studentCode,
        String studentName,
        /** Which of the quiz's (possibly several) released classrooms this student sits in. */
        UUID classroomId,
        String className,
        String status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        Double earnedScore,
        Double totalScore
) {
}
