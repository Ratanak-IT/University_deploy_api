package com.universitymanagement.admin.dto.response;

import java.util.List;

public record AdminDetailResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        List<String> roles,
        String adminCode,
        String position,
        String department
) {
}
