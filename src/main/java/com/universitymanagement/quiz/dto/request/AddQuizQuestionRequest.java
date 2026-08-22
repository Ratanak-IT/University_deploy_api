package com.universitymanagement.quiz.dto.request;

import com.universitymanagement.quiz.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddQuizQuestionRequest(
        @NotBlank(message = "Question text is required")
        String questionText,

        List<String> options,

        /**
         * Zero-based index of the correct option. Preferred over
         * {@code correctAnswer} for choice questions; when both are sent the
         * index wins.
         */
        Integer correctOptionIndex,

        /** Defaults to MULTIPLE_CHOICE when omitted. */
        QuestionType type,

        /**
         * The correct answer as text.
         *
         * <p>Required for SHORT_ANSWER. Optional for choice questions, where it
         * only locates the index when {@code correctOptionIndex} is absent —
         * which keeps older clients working.
         */
        String correctAnswer,

        @NotNull(message = "Score is required")
        Double score,

        Integer questionOrder
) {
}
