package com.universitymanagement.grading.dto.request;

import com.universitymanagement.grading.entity.ScoreStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

/**
 * A batch of gradebook edits. Cells from different assessments can be mixed in
 * one call so the grid saves as a unit.
 */
public record SaveScoresRequest(
        @NotEmpty(message = "at least one score is required")
        List<@Valid ScoreItem> scores
) {
    public record ScoreItem(
            @NotNull(message = "assessmentId is required")
            UUID assessmentId,

            @NotNull(message = "studentId is required")
            UUID studentId,

            /** Null clears the mark back to "not graded yet". */
            @PositiveOrZero(message = "score cannot be negative")
            Double score,

            /** Defaults to GRADED when omitted. */
            ScoreStatus status,

            String feedback
    ) {
    }
}
