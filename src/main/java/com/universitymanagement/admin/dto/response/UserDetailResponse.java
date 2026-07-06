package com.universitymanagement.admin.dto.response;

import java.util.List;

public record UserDetailResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified,
        Long createdTimestamp,
        List<String> roles
) {
}
