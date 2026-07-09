package com.universitymanagement.classroom.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ClassroomCreateRequest(
        @NotBlank(message = "Class name is required")
        String className,

        @NotNull(message = "Teacher ID is required")
        UUID subjectId,
        UUID teacherId,

        @NotNull(message = "Program ID is required")
        UUID programId,

        @NotBlank(message = "Academic year is required")
        String academicYear,

        @Min(value = 1, message = "Semester must be 1 or 2")
        @Max(value = 2, message = "Semester must be 1 or 2")
        Integer semester,

        @Min(value = 1, message = "Year level must be between 1 and 4")
        @Max(value = 4, message = "Year level must be between 1 and 4")
        Integer yearLevel,

        String room,

        LocalDate startDate,

        LocalDate endDate
) {
}
