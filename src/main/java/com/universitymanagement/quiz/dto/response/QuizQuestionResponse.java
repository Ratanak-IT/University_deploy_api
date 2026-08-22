package com.universitymanagement.quiz.dto.response;

import java.util.List;
import java.util.UUID;

import com.universitymanagement.quiz.entity.QuestionType;

public record QuizQuestionResponse(
        UUID questionId,
        String questionText,
        List<String> options,
        /**
         * How to render and answer this question.
         *
         * <p>Safe to expose: it says what kind of input to show, never what the
         * answer is. Neither the correct option nor its index appears here.
         */
        QuestionType type,
        Double score,
        Integer questionOrder
) {
}
