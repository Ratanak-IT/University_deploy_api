package com.universitymanagement.quiz.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** One classroom a quiz has been released to, with that section's window. */
public record QuizClassroomResponse(
        UUID assignmentId,
        UUID classroomId,
        String className,
        String classCode,
        String subjectName,
        /** Null when the section follows the quiz's own window. */
        LocalDateTime availableFrom,
        LocalDateTime availableTo
) {
}
