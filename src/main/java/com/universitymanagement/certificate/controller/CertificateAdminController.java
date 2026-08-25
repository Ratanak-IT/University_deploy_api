package com.universitymanagement.certificate.controller;

import com.universitymanagement.certificate.dto.request.IssueBatchRequest;
import com.universitymanagement.certificate.dto.request.RejectRequestRequest;
import com.universitymanagement.certificate.dto.request.RevokeCertificateRequest;
import com.universitymanagement.certificate.dto.request.SaveFieldsRequest;
import com.universitymanagement.certificate.dto.request.SaveTemplateRequest;
import com.universitymanagement.certificate.dto.response.*;
import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.service.CertificateAdminService;
import com.universitymanagement.certificate.service.CertificateRequestAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Templates and issuing. Registrar only. */
@RestController
@RequestMapping("/api/v1/admin/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CertificateAdminController {

    private final CertificateAdminService certificateAdminService;
    private final CertificateRequestAdminService requestAdminService;

    /* ---- templates ---- */

    @GetMapping("/templates")
    public List<TemplateResponse> listTemplates() {
        return certificateAdminService.listTemplates();
    }

    @GetMapping("/templates/{templateId}")
    public TemplateResponse getTemplate(@PathVariable UUID templateId) {
        return certificateAdminService.getTemplate(templateId);
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse createTemplate(@Valid @RequestBody SaveTemplateRequest request) {
        return certificateAdminService.createTemplate(request);
    }

    @PutMapping("/templates/{templateId}")
    public TemplateResponse updateTemplate(@PathVariable UUID templateId,
                                           @Valid @RequestBody SaveTemplateRequest request) {
        return certificateAdminService.updateTemplate(templateId, request);
    }

    /** Uploads an .html file as the template body — the "bring your own design" path. */
    @PostMapping(value = "/templates/{templateId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemplateResponse uploadTemplateBody(@PathVariable UUID templateId,
                                               @RequestPart("file") MultipartFile file) {
        return certificateAdminService.uploadTemplateBody(templateId, file);
    }

    /**
     * Uploads a finished design — a PDF exported from Word, Canva or
     * Illustrator, or a flat image — and reports what was found in it.
     *
     * <p>A PDF records where its text sits, so every {{placeholder}} the
     * designer typed is located exactly and the fields are placed for them. An
     * image carries no text, so the boxes are dragged on afterwards.
     */
    @PostMapping(value = "/templates/{templateId}/design",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DesignUploadResponse uploadDesign(@PathVariable UUID templateId,
                                             @RequestPart("file") MultipartFile file) {
        return certificateAdminService.uploadDesign(templateId, file);
    }

    /** Stores the field positions the editor produced. */
    @PutMapping("/templates/{templateId}/fields")
    public TemplateResponse saveFields(@PathVariable UUID templateId,
                                       @Valid @RequestBody SaveFieldsRequest request) {
        return certificateAdminService.saveFields(templateId, request);
    }

    /** The uploaded design itself, for the editor to show beneath its boxes. */
    @GetMapping("/templates/{templateId}/design")
    public ResponseEntity<byte[]> getDesign(@PathVariable UUID templateId) {
        TemplateResponse template = certificateAdminService.getTemplate(templateId);
        byte[] bytes = certificateAdminService.getDesignBytes(templateId);

        MediaType type = MediaType.APPLICATION_PDF;
        if ("IMAGE".equals(template.assetKind())) {
            String name = template.assetOriginalName() != null
                    ? template.assetOriginalName().toLowerCase() : "";
            type = name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        }

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }

    /**
     * A filled PDF against sample or real data, so the placement is checked on
     * screen before a cohort of certificates carries a mistake.
     */
    @GetMapping(value = "/templates/{templateId}/design/preview",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewOverlay(@PathVariable UUID templateId,
                                                 @RequestParam(required = false) UUID studentId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf")
                .body(certificateAdminService.previewOverlay(templateId, studentId));
    }

    @PostMapping("/templates/{templateId}/activate")
    public TemplateResponse activateTemplate(@PathVariable UUID templateId) {
        return certificateAdminService.activateTemplate(templateId);
    }

    @DeleteMapping("/templates/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID templateId) {
        certificateAdminService.deleteTemplate(templateId);
    }

    /** The placeholder keys the editor offers. */
    @GetMapping("/placeholders")
    public List<PlaceholderResponse> listPlaceholders() {
        return certificateAdminService.listPlaceholders();
    }

    /**
     * Renders the template without issuing anything, so a mistake is caught
     * before a cohort of certificates carries it.
     */
    @GetMapping(value = "/templates/{templateId}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String previewTemplate(@PathVariable UUID templateId,
                                  @RequestParam(required = false) UUID studentId) {
        return certificateAdminService.previewTemplate(templateId, studentId);
    }

    /* ---- issuing ---- */

    /** Awards a certificate to a whole programme/year cohort. */
    @PostMapping("/issue")
    public IssueBatchResponse issueBatch(@Valid @RequestBody IssueBatchRequest request) {
        return certificateAdminService.issueBatch(request);
    }

    @GetMapping("/issued")
    public List<IssuedCertificateResponse> listIssued(
            @RequestParam(required = false) CertificateType type,
            @RequestParam(required = false) UUID programId) {
        return certificateAdminService.listIssued(type, programId);
    }

    /** Attaches a scanned signed original to an award. */
    @PostMapping(value = "/issued/{issuedId}/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IssuedCertificateResponse attachFile(@PathVariable UUID issuedId,
                                                @RequestPart("file") MultipartFile file) {
        return certificateAdminService.attachFile(issuedId, file);
    }

    /* ---- student requests ---- */

    /**
     * The request inbox, pending first.
     *
     * <p>Each row carries whether it can be approved and which template would
     * print it, so the decision is made from the list rather than by clicking
     * to find out.
     */
    @GetMapping("/requests")
    public List<AdminCertificateRequestResponse> listRequests(
            @RequestParam(required = false) CertificateStatus status,
            @RequestParam(required = false) CertificateType type,
            @RequestParam(required = false) UUID programId) {
        return requestAdminService.list(status, type, programId);
    }

    /**
     * Approves a request by issuing the certificate.
     *
     * @param override award it despite a failed eligibility check
     */
    @PostMapping("/requests/{requestId}/approve")
    public AdminCertificateRequestResponse approveRequest(
            @PathVariable UUID requestId,
            @RequestParam(defaultValue = "false") boolean override) {
        return requestAdminService.approve(requestId, override);
    }

    @PostMapping("/requests/{requestId}/reject")
    public AdminCertificateRequestResponse rejectRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectRequestRequest request) {
        return requestAdminService.reject(requestId, request);
    }

    @PostMapping("/issued/{issuedId}/revoke")
    public IssuedCertificateResponse revoke(@PathVariable UUID issuedId,
                                            @Valid @RequestBody RevokeCertificateRequest request) {
        return certificateAdminService.revoke(issuedId, request);
    }
}
