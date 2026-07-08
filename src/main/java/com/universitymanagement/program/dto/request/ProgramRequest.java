package com.universitymanagement.program.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProgramRequest(
        @NotBlank(message = "Program name is required")
        String programName,
        @NotBlank(message = "Degree level is required")
        String degreeLevel,
        @NotNull(message = "Duration years is required")
        Integer durationYears
) {
}
