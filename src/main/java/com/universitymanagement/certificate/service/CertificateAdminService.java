package com.universitymanagement.certificate.service;

import com.universitymanagement.certificate.dto.request.IssueBatchRequest;
import com.universitymanagement.certificate.dto.request.RevokeCertificateRequest;
import com.universitymanagement.certificate.dto.request.SaveFieldsRequest;
import com.universitymanagement.certificate.dto.request.SaveTemplateRequest;
import com.universitymanagement.certificate.dto.response.*;
import com.universitymanagement.certificate.entity.CertificateType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Everything the registrar does with certificates. */
public interface CertificateAdminService {

    /* ---- templates ---- */

    List<TemplateResponse> listTemplates();

    TemplateResponse getTemplate(UUID templateId);

    TemplateResponse createTemplate(SaveTemplateRequest request);

    TemplateResponse updateTemplate(UUID templateId, SaveTemplateRequest request);

    /** Uploads an .html file as the template body. */
    TemplateResponse uploadTemplateBody(UUID templateId, MultipartFile file);

    /**
     * Uploads a PDF or image design and reports the placeholders found in it.
     *
     * <p>For a PDF this locates each placeholder exactly, because a PDF records
     * where its text sits. For an image there is nothing to read, so the fields
     * are placed by hand afterwards.
     */
    DesignUploadResponse uploadDesign(UUID templateId, MultipartFile file);

    /** Stores where each value prints on the uploaded design. */
    TemplateResponse saveFields(UUID templateId, SaveFieldsRequest request);

    /** The uploaded design itself, for the editor to show behind its overlay. */
    byte[] getDesignBytes(UUID templateId);

    /** Renders the overlay template against sample or real data, as a PDF. */
    byte[] previewOverlay(UUID templateId, UUID studentId);

    /** Makes this the template issuing uses, archiving whatever it replaces. */
    TemplateResponse activateTemplate(UUID templateId);

    void deleteTemplate(UUID templateId);

    /** The keys a template may use, for the editor to list. */
    List<PlaceholderResponse> listPlaceholders();

    /** Renders the template against one real student, without issuing anything. */
    String previewTemplate(UUID templateId, UUID studentId);

    /* ---- issuing ---- */

    /** Awards a certificate to a whole programme/year cohort at once. */
    IssueBatchResponse issueBatch(IssueBatchRequest request);

    /**
     * Awards one certificate, through the same path a batch uses.
     *
     * <p>This is what approving a student's request runs, so a requested
     * certificate is indistinguishable from a batch-issued one.
     *
     * @param override issue despite a failed eligibility check
     */
    IssuedCertificateResponse issueOne(UUID studentId, CertificateType certificateType,
                                       boolean override);

    /** Attaches a scanned paper original to an already-issued certificate. */
    IssuedCertificateResponse attachFile(UUID issuedId, MultipartFile file);

    List<IssuedCertificateResponse> listIssued(CertificateType type, UUID programId);

    IssuedCertificateResponse revoke(UUID issuedId, RevokeCertificateRequest request);

    /* ---- public ---- */

    VerificationResponse verify(String verificationCode);
}
