package com.universitymanagement.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record TransferCreditRequest(
        @NotNull UUID studentId,
        UUID subjectId,
        @NotBlank String sourceInstitution,
        @NotNull @Positive Double credits,
        LocalDate grantedDate
) {
}
