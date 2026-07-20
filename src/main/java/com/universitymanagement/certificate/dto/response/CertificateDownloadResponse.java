package com.universitymanagement.certificate.dto.response;

import java.util.UUID;

public record CertificateDownloadResponse(
        UUID requestId,
        String fileName,
        String downloadUrl
) {
}
