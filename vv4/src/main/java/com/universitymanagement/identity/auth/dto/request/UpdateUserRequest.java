package com.universitymanagement.identity.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        String firstName,
        String lastName,

        @Email
        String email,

        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String phoneNumber
) {
}
