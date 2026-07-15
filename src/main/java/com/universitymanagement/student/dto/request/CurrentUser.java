package com.universitymanagement.student.dto.request;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String keycloakId,
        String email,
        Set<String> roles
) {}