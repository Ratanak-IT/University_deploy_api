package com.universitymanagement.identity.auth.dto.response;

import com.universitymanagement.admin.dto.GenderOption;
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
        GenderOption gender,
        RoleName role,
        boolean enabled
) {
}
