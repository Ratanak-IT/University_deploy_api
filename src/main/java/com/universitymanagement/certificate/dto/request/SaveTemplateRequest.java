package com.universitymanagement.certificate.dto.request;

import com.universitymanagement.certificate.entity.CertificateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Creates or edits a certificate template. */
public record SaveTemplateRequest(
        @NotNull(message = "certificateType is required")
        CertificateType certificateType,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotBlank(message = "the template body is required")
        String bodyHtml,

        /** Null makes the template serve every programme. */
        UUID programId,

        @Size(max = 300, message = "description must be at most 300 characters")
        String description
) {
}
