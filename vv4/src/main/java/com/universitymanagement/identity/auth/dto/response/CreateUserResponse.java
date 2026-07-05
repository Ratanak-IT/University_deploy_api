package com.universitymanagement.identity.auth.dto.response;

import com.universitymanagement.identity.enums.RoleName;

import java.time.LocalDate;
import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String keycloakId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        RoleName role,
        boolean enabled
) {
}
