package com.universitymanagement.program.dto.response;

import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String programName,
        String degreeLevel,
        Integer durationYears
) {
}
