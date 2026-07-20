package com.universitymanagement.teacher.dto.request;

import com.universitymanagement.admin.dto.GenderOption;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTeacherRequest(
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

        String specialization,

        List<UUID> departmentIds,

        String position
) {
}
