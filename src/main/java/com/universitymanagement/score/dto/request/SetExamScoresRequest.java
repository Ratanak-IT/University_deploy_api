package com.universitymanagement.score.dto.request;

import com.universitymanagement.score.entity.ExamType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record SetExamScoresRequest(
        @NotNull(message = "examType is required")
        ExamType examType,

        @NotNull(message = "maxScore is required")
        @Positive(message = "maxScore must be greater than 0")
        Double maxScore,

        @NotEmpty(message = "scores cannot be empty")
        List<@Valid StudentScore> scores
) {
    public record StudentScore(
            @NotNull(message = "studentId is required")
            UUID studentId,

            @NotNull(message = "score is required")
            @PositiveOrZero(message = "score cannot be negative")
            Double score
    ) {
    }
}