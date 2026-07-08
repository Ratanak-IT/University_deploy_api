package com.universitymanagement.curriculum.dto.response;

import java.util.UUID;

public record CurriculumResponse(
        UUID curriculumId,
        Integer semester,
        Integer yearLevel,
        UUID programId,
        String programName,
        UUID subjectId,
        String subjectName,
        String subjectCode,
        Double credit
) {
}
