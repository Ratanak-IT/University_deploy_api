package com.universitymanagement.notification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String title,
        String message,
        String type,
        String context,
        String actor,
        boolean isRead,
        LocalDateTime createdAt
) {
}
