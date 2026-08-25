package com.universitymanagement.certificate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Turning a request down.
 *
 * <p>The reason is required. A student who is refused with no explanation only
 * asks again, so the registrar handles it twice — and a rejection that cannot
 * be explained is usually one that should not have been made.
 */
public record RejectRequestRequest(
        @NotBlank(message = "a reason is required")
        @Size(max = 500, message = "keep the reason under 500 characters")
        String reason
) {
}
