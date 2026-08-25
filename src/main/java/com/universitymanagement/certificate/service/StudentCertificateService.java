package com.universitymanagement.certificate.service;

import com.universitymanagement.certificate.dto.response.CertificateDownloadResponse;
import com.universitymanagement.certificate.dto.response.IssuedCertificateResponse;

import java.util.List;
import java.util.UUID;

/**
 * What a student can reach.
 *
 * <p>Every method reads the issued-certificates table, and a row only appears
 * there once the registrar has awarded one. That is the whole access rule: no
 * separate "published" flag to forget to set, and nothing to see before
 * approval because there is nothing there.
 */
public interface StudentCertificateService {

    List<IssuedCertificateResponse> getMyCertificates(UUID studentId);

    /** The stored document, exactly as awarded. */
    String getCertificateHtml(UUID studentId, UUID issuedId);

    /** A link to the scanned original, where one was attached. */
    CertificateDownloadResponse download(UUID studentId, UUID issuedId);

    /**
     * Same file as {@link #download}, but opened inline rather than forced as
     * an attachment — for a PDF certificate, this is what lets a student look
     * at it before deciding to save it.
     */
    CertificateDownloadResponse preview(UUID studentId, UUID issuedId);
}
