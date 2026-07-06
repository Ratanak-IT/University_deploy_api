package com.universitymanagement.identity.auth.dto.request;

import com.universitymanagement.admin.dto.GenderOption;
import com.universitymanagement.identity.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

        LocalDate dateOfBirth,

        String phoneNumber,
        @NotNull(message = "Gender cannot be null")
        GenderOption gender,


        @NotNull
        RoleName role
) {
}
