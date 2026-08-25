package com.universitymanagement.certificate.service;

import com.universitymanagement.certificate.dto.request.RejectRequestRequest;
import com.universitymanagement.certificate.dto.response.AdminCertificateRequestResponse;
import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.CertificateType;

import java.util.List;
import java.util.UUID;

/**
 * The registrar's side of student certificate requests.
 *
 * <p>Requests are the inbox, not the machinery. Approving one runs the same
 * issuing path a cohort batch does, so nothing here mints a certificate of its
 * own — it decides, and delegates.
 */
public interface CertificateRequestAdminService {

    /** Pending first, since those are the ones that need a decision. */
    List<AdminCertificateRequestResponse> list(CertificateStatus status,
                                               CertificateType type,
                                               UUID programId);

    /**
     * Issues the certificate and marks the request approved.
     *
     * @param override award it despite a failed eligibility check
     */
    AdminCertificateRequestResponse approve(UUID requestId, boolean override);

    AdminCertificateRequestResponse reject(UUID requestId, RejectRequestRequest request);
}
