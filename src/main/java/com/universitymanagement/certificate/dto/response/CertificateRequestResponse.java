package com.universitymanagement.certificate.dto.response;

import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.CertificateType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CertificateRequestResponse(
        UUID requestId,
        CertificateType certificateType,
        String reason,
        CertificateStatus status,
        String rejectReason,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        boolean downloadable
) {
}
