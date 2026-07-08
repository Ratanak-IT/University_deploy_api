package com.universitymanagement.subject.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SubjectResponse(
        String subjectCode,
        String subjectName,
        Double credit,
        UUID departmentId
) {
}
