package com.universitymanagement.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Calls a session off. A reason is required — the register has to explain the gap. */
public record CancelSessionRequest(
        @NotBlank(message = "a reason is required when cancelling a session")
        @Size(max = 300, message = "reason must be at most 300 characters")
        String reason
) {
}
