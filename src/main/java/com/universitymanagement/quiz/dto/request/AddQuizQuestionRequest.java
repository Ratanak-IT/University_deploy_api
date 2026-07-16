package com.universitymanagement.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddQuizQuestionRequest(
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
