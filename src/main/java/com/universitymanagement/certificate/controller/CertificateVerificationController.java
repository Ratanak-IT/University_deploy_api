package com.universitymanagement.certificate.controller;

import com.universitymanagement.certificate.dto.response.VerificationResponse;
import com.universitymanagement.certificate.service.CertificateAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public certificate verification.
 *
 * <p>Deliberately unauthenticated: the point is that an employer holding a
 * printed certificate can confirm it without an account. The response is
 * limited to what proves authenticity — name, award, number, date — and never
 * grades or contact details, so the endpoint cannot be turned into a way to
 * read student records.
 */
@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
public class CertificateVerificationController {

    private final CertificateAdminService certificateAdminService;

    @GetMapping("/{verificationCode}")
    public VerificationResponse verify(@PathVariable String verificationCode) {
        return certificateAdminService.verify(verificationCode);
    }
}
