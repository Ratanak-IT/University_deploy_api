package com.universitymanagement.admin.dto.response;

import java.time.LocalDateTime;

public record LoginHistoryResponse(
        Long id,
        String deviceType,
        String ipAddress,
        String userAgent,
        LocalDateTime loginTime,
        LocalDateTime lastRefreshed,
        LocalDateTime expiresAt,
        LocalDateTime logoutTime,
        Boolean isActive,
        Boolean revoked
) {
}
