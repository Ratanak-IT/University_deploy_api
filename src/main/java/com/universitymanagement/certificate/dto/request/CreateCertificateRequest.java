package com.universitymanagement.certificate.dto.request;

import com.universitymanagement.certificate.entity.CertificateType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCertificateRequest(
        @NotNull(message = "Certificate type is required")
                CertificateType certificateType,

        @Size(max = 500)
        String reason
) {
}
