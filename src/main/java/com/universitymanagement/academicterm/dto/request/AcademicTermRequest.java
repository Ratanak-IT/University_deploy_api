package com.universitymanagement.academicterm.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AcademicTermRequest(
        @NotBlank String name,
        @NotBlank String academicYear,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate addDropDeadline,
        Boolean isActive
) {
}
