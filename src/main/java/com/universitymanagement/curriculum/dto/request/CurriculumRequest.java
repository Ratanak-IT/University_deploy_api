package com.universitymanagement.curriculum.dto.request;

import com.universitymanagement.curriculum.entity.CourseType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CurriculumRequest(
        @NotNull(message = "Semester cannot be null")
        @Min(value = 1, message = "Semester must be at least 1")
        @Max(value = 12, message = "Semester must not exceed 12")
        Integer semester,

        @NotNull(message = "Year level cannot be null")
        @Min(value = 1, message = "Year level must be at least 1")
        @Max(value = 6, message = "Year level must not exceed 6")
        Integer yearLevel,

        @NotNull(message = "Program ID cannot be null")
        UUID programId,

        @NotNull(message = "Subject ID cannot be null")
        UUID subjectId,

        CourseType courseType,

        UUID prerequisiteSubjectId,

        Integer lectureHours,

        Integer labHours
) {
}
