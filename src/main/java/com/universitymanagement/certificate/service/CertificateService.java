package com.universitymanagement.certificate.service;

import com.universitymanagement.certificate.dto.request.CreateCertificateRequest;
import com.universitymanagement.certificate.dto.response.CertificateDownloadResponse;
import com.universitymanagement.certificate.dto.response.CertificateRequestResponse;

import java.util.List;
import java.util.UUID;

public interface CertificateService {
    List<CertificateRequestResponse> getRequestsForStudent(UUID studentId);
    CertificateRequestResponse createRequest(UUID studentId, CreateCertificateRequest request);
    CertificateDownloadResponse downloadApprovedCertificate(UUID studentId, UUID requestId);
}
