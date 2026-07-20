package com.universitymanagement.admin.dto.request;

import com.universitymanagement.identity.entity.Role;

public record UserSummaryRequest(
        String id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Role role
) {
}
