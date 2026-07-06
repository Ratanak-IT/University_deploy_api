package com.universitymanagement.admin.dto.response;

public record UserSummaryResponse(
        String id,
         String username,
         String email,
         String firstName,
         String lastName,
         boolean enabled
) {
}
