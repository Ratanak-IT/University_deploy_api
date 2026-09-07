package com.universitymanagement.classroom.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record WaitlistEntryResponse(
        UUID studentId,
        String studentCode,
        String studentName,
        LocalDateTime requestedAt
) {
}
