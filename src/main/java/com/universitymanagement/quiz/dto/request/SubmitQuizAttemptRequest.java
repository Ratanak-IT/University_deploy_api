package com.universitymanagement.quiz.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitQuizAttemptRequest(
        @NotEmpty(message = "Answers cannot be empty")
        List<AnswerItem> answers
) {
    public record AnswerItem(
            @NotNull UUID questionId,
            String answer
    ) {
    }
}
