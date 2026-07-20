package com.universitymanagement.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateQuizRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        LocalDateTime startAt,
        LocalDateTime endAt,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @Min(value = 1, message = "Max attempts must be at least 1")
        Integer maxAttempts,
        List<@Valid QuestionItem> questions
) {
    public record QuestionItem(
            @NotBlank(message = "Question text is required")
            String questionText,

            @NotEmpty(message = "Options cannot be empty")
            List<String> options,

            @NotBlank(message = "Correct answer is required")
            String correctAnswer,

            @NotNull(message = "Score is required")
            Double score,

            Integer questionOrder
    ) {
    }
}
