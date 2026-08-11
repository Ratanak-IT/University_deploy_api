package com.universitymanagement.admin.dto.response;

import java.util.List;

public record UserSummaryResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        List<String> roles
) {
}
