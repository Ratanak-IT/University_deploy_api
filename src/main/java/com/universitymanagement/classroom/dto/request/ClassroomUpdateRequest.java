package com.universitymanagement.classroom.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record ClassroomUpdateRequest(
        @NotBlank(message = "Class name is required")
        String className,

        UUID teacherId,

        UUID subjectId,

        UUID programId,

        @NotBlank(message = "Academic year is required")
        String academicYear,

        @Min(1)
        @Max(2)
        Integer semester,

        @Min(1)
        @Max(4)
        Integer yearLevel,

        String room,

        LocalDate startDate,

        LocalDate endDate
) {
}
