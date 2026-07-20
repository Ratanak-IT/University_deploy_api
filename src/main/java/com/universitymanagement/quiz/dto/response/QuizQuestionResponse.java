package com.universitymanagement.quiz.dto.response;

import java.util.List;
import java.util.UUID;

public record QuizQuestionResponse(
        UUID questionId,
        String questionText,
        List<String> options,
        Double score,
        Integer questionOrder
) {
}
