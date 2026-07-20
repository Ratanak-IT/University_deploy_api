package com.universitymanagement.subject.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SubjectRequest(

        @NotBlank(message = "Subject name cannot be blank")
        String subjectName,

        @NotNull(message = "Subject credit cannot be null")
        @Positive(message = "Subject credit must be greater than 0")
        Double credit,

        @NotNull(message = "Department ID cannot be null")
        UUID departmentId
) {
}
