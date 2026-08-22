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

            /**
             * The option the student picked, zero-based.
             *
             * <p>Preferred for choice questions. Sending the index rather than
             * the option's text means grading no longer depends on the two
             * strings still matching.
             */
            Integer selectedOptionIndex,

            /** The typed answer for SHORT_ANSWER, or a legacy text choice. */
            String answer
    ) {
    }
}
