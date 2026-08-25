package com.universitymanagement.certificate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Withdraws a certificate. A reason is required — the record has to explain itself. */
public record RevokeCertificateRequest(
        @NotBlank(message = "a reason is required when revoking a certificate")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
