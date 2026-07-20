package com.universitymanagement.student.dto.request;

import com.universitymanagement.admin.dto.GenderOption;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record CreateStudentRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        String confirmPassword,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String phoneNumber,

        @NotNull(message = "Date of birth cannot be null")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender cannot be null")
        GenderOption gender,

        @NotBlank(message = "Academic year is required")
        String academicYear,

        @NotNull(message = "Year level is required")
        @Min(1) @Max(10)
        Integer yearLevel,

        @NotNull(message = "Semester is required")
        @Min(1) @Max(3)
        Integer semester,

        UUID programId,

        LocalDate enrollmentDate
) {
}
