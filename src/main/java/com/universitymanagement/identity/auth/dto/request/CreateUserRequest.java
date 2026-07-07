package com.universitymanagement.identity.auth.dto.request;

import com.universitymanagement.admin.dto.GenderOption;
import com.universitymanagement.identity.enums.RoleName;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateUserRequest(
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

        @NotNull
        RoleName role
) {
}
