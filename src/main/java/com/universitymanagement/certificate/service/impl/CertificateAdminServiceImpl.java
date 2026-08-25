package com.universitymanagement.certificate.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.certificate.dto.request.SaveFieldsRequest;
import com.universitymanagement.certificate.dto.request.IssueBatchRequest;
import com.universitymanagement.certificate.dto.request.RevokeCertificateRequest;
import com.universitymanagement.certificate.dto.request.SaveTemplateRequest;
import com.universitymanagement.certificate.dto.response.*;
import com.universitymanagement.certificate.entity.*;
import com.universitymanagement.certificate.render.CertificateRenderer;
import com.universitymanagement.certificate.render.CertificateComposer;
import com.universitymanagement.certificate.render.DetectedField;
import com.universitymanagement.certificate.render.DocxPlaceholderScanner;
import com.universitymanagement.certificate.render.FieldPlacement;
import com.universitymanagement.certificate.render.PdfPlaceholderScanner;
import com.universitymanagement.certificate.repository.CertificateTemplateRepository;
import com.universitymanagement.certificate.repository.IssuedCertificateRepository;
import com.universitymanagement.certificate.service.CertificateAdminService;
import com.universitymanagement.certificate.service.CertificateEligibility;
import com.universitymanagement.certificate.service.CertificateNumberGenerator;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.notification.service.NotificationService;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.program.repository.ProgramRepository;
import com.universitymanagement.student.dto.response.StudentAcademicSummaryResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.student.service.StudentCohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateAdminServiceImpl implements CertificateAdminService {

    private static final DateTimeFormatter ISSUE_DATE =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /** Generous for a one-page certificate; stops a stray export filling a TEXT column. */
    private static final long MAX_TEMPLATE_BYTES = 512L * 1024L;

    private final CertificateTemplateRepository templateRepository;
    private final IssuedCertificateRepository issuedRepository;
    private final ProgramRepository programRepository;
    private final StudentRepository studentRepository;
    private final StudentCohortService cohortService;
    private final CertificateRenderer renderer;
    private final CertificateNumberGenerator numbers;
    private final NotificationService notificationService;
    private final MinioService minioService;
    private final CertificateEligibility eligibility;
    private final PdfPlaceholderScanner pdfScanner;
    private final DocxPlaceholderScanner docxScanner;
    private final CertificateComposer composer;
    private final ObjectMapper objectMapper;

    @Value("${app.public-url:https://careerpatch.site}")
    private String publicUrl;

    @Value("${app.university-name:University of Technology}")
    private String universityName;

    /* ------------------------------------------------------------------ */
    /* Templates                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public List<TemplateResponse> listTemplates() {
        return templateRepository.findByOrderByCertificateTypeAscNameAsc()
                .stream().map(this::toTemplateResponse).toList();
    }

    @Override
    public TemplateResponse getTemplate(UUID templateId) {
        return toTemplateResponse(requireTemplate(templateId));
    }

    @Override
    @Transactional
    public TemplateResponse createTemplate(SaveTemplateRequest request) {
        CertificateTemplate template = new CertificateTemplate();
        apply(template, request);
        template.setVersion(1);
        template.setStatus(TemplateStatus.DRAFT);
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(UUID templateId, SaveTemplateRequest request) {
        CertificateTemplate template = requireTemplate(templateId);

        // Editing a live template bumps its version, so a certificate issued
        // yesterday still records which wording produced it.
        if (template.getStatus() == TemplateStatus.ACTIVE
                && !Objects.equals(template.getBodyHtml(), request.bodyHtml())) {
            template.setVersion(template.getVersion() + 1);
        }

        apply(template, request);
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse uploadTemplateBody(UUID templateId, MultipartFile file) {
        CertificateTemplate template = requireTemplate(templateId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded.");
        }

        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("")
                .toLowerCase(Locale.ROOT);
        if (!name.endsWith(".html") && !name.endsWith(".htm")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload an .html file. The body is rendered as HTML, not converted "
                            + "from another format.");
        }
        if (file.getSize() > MAX_TEMPLATE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The template is larger than 512 KB. Embed images as data URIs "
                            + "or link to them instead.");
        }

        try {
            String html = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (template.getStatus() == TemplateStatus.ACTIVE) {
                template.setVersion(template.getVersion() + 1);
            }
            template.setBodyHtml(html);
            return toTemplateResponse(templateRepository.save(template));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read the uploaded file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TemplateResponse activateTemplate(UUID templateId) {
        CertificateTemplate template = requireTemplate(templateId);

        // An uploaded design carries no HTML body, so it is checked on what it
        // does have: a stored file and at least one placed field.
        if (template.getRenderMode() == RenderMode.OVERLAY) {
            requireOverlayReady(template);
        } else {
            List<String> unknown = renderer.unknownPlaceholders(template.getBodyHtml());
            if (!unknown.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This template uses placeholders that will never be filled in: "
                                + String.join(", ", unknown) + ". Fix them before activating it.");
            }
        }

        // Exactly one active template per type and programme, so issuing never
        // has to guess which of two designs was meant.
        UUID programId = template.getProgram() != null ? template.getProgram().getId() : null;
        for (CertificateTemplate other : templateRepository
                .findByCertificateTypeAndStatus(template.getCertificateType(), TemplateStatus.ACTIVE)) {
            UUID otherProgram = other.getProgram() != null ? other.getProgram().getId() : null;
            if (Objects.equals(otherProgram, programId)
                    && !other.getTemplateId().equals(templateId)) {
                other.setStatus(TemplateStatus.ARCHIVED);
                templateRepository.save(other);
            }
        }

        template.setStatus(TemplateStatus.ACTIVE);
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID templateId) {
        CertificateTemplate template = requireTemplate(templateId);
        if (template.getStatus() == TemplateStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This template is in use. Activate another one first.");
        }
        templateRepository.delete(template);
    }

    @Override
    public List<PlaceholderResponse> listPlaceholders() {
        return CertificateRenderer.SUPPORTED.entrySet().stream()
                .map(e -> new PlaceholderResponse(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public String previewTemplate(UUID templateId, UUID studentId) {
        CertificateTemplate template = requireTemplate(templateId);

        if (studentId == null) {
            return renderer.render(template.getBodyHtml(), sampleValues());
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found: " + studentId));

        return renderer.render(template.getBodyHtml(),
                valuesFor(student, summaryFor(student), "PREVIEW-00000", "PREV-IEWX-0000"));
    }

    /* ------------------------------------------------------------------ */
    /* Uploaded designs                                                    */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional
    public DesignUploadResponse uploadDesign(UUID templateId, MultipartFile file) {
        CertificateTemplate template = requireTemplate(templateId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded.");
        }

        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("design");
        String lower = name.toLowerCase(Locale.ROOT);

        try {
            byte[] bytes = file.getBytes();

            // Word is handled first, because it is the one upload that cannot be
            // used as it is — and saying so with the placeholder list in hand is
            // far more useful than simply refusing the file.
            if (lower.endsWith(".docx")) {
                List<String> keys = docxScanner.scan(bytes);
                return new DesignUploadResponse(
                        "DOCX", name, null, null, List.of(),
                        keys.stream()
                                .filter(k -> !CertificateRenderer.SUPPORTED.containsKey(k))
                                .toList(),
                        docxGuidance(keys));
            }

            if (lower.endsWith(".pdf")) {
                List<DetectedField> detected = pdfScanner.scan(bytes);
                float[] size = pdfScanner.pageSize(bytes);

                storeDesign(template, file, "PDF", size[0], size[1]);
                // The detected positions become the starting field list, so a
                // design that carried placeholders needs no manual placement.
                template.setFieldsJson(writeFields(
                        detected.stream().map(FieldPlacement::from).toList()));
                templateRepository.save(template);

                return new DesignUploadResponse(
                        "PDF", name, size[0], size[1], detected,
                        detected.stream()
                                .map(DetectedField::key)
                                .filter(k -> !CertificateRenderer.SUPPORTED.containsKey(k))
                                .distinct()
                                .toList(),
                        detected.isEmpty()
                                ? "No placeholders were found in this PDF. Drag a box onto "
                                        + "the design for each value you want printed."
                                : null);
            }

            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                storeDesign(template, file, "IMAGE", null, null);
                templateRepository.save(template);

                return new DesignUploadResponse(
                        "IMAGE", name, null, null, List.of(), List.of(),
                        "An image has no text to read, so nothing can be detected. Drag a "
                                + "box onto the design for each value you want printed.");
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload a PDF, PNG or JPEG design. A Word file can be inspected but "
                            + "must be saved as PDF before it can be used.");

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read the uploaded design: " + e.getMessage(), e);
        }
    }

    /**
     * A .docx cannot be filled dependably, so this message has to earn its
     * place: it reports what the design actually contains and what to do next,
     * rather than leaving the registrar at a dead end.
     */
    private String docxGuidance(List<String> keys) {
        if (keys.isEmpty()) {
            return "No placeholders were found in this Word file. Add them where you want "
                    + "the details printed, then save as PDF and upload that.";
        }
        return "Found " + keys.size() + " placeholder(s): "
                + String.join(", ", keys)
                + ". A Word file has no fixed layout, so save it as PDF and upload that "
                + "instead — the positions are then read automatically.";
    }

    private void storeDesign(CertificateTemplate template, MultipartFile file,
                             String kind, Float width, Float height) {
        template.setAssetObjectName(minioService.uploadAsset(file));
        template.setAssetOriginalName(file.getOriginalFilename());
        template.setAssetKind(kind);
        template.setAssetWidth(width);
        template.setAssetHeight(height);
        template.setRenderMode(RenderMode.OVERLAY);

        if (template.getStatus() == TemplateStatus.ACTIVE) {
            template.setVersion(template.getVersion() + 1);
        }
    }

    @Override
    @Transactional
    public TemplateResponse saveFields(UUID templateId, SaveFieldsRequest request) {
        CertificateTemplate template = requireTemplate(templateId);

        List<FieldPlacement> fields = request.fields().stream()
                .map(f -> new FieldPlacement(
                        f.key(),
                        f.page() != null ? f.page() : 1,
                        f.x(),
                        f.y(),
                        f.width() != null ? f.width() : 200f,
                        f.fontSize() != null ? f.fontSize() : 14f,
                        f.align() != null ? f.align() : "LEFT",
                        f.color() != null ? f.color() : "#000000",
                        Boolean.TRUE.equals(f.bold())))
                .toList();

        List<String> unknown = fields.stream()
                .map(FieldPlacement::key)
                .filter(k -> !CertificateRenderer.SUPPORTED.containsKey(k))
                .distinct()
                .toList();
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "These fields have no value to fill them: " + String.join(", ", unknown));
        }

        template.setFieldsJson(writeFields(fields));
        if (template.getStatus() == TemplateStatus.ACTIVE) {
            template.setVersion(template.getVersion() + 1);
        }

        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    public byte[] getDesignBytes(UUID templateId) {
        CertificateTemplate template = requireTemplate(templateId);
        if (template.getAssetObjectName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "This template has no uploaded design.");
        }
        return minioService.getAssetBytes(template.getAssetObjectName());
    }

    @Override
    public byte[] previewOverlay(UUID templateId, UUID studentId) {
        CertificateTemplate template = requireTemplate(templateId);

        Map<String, String> values;
        if (studentId == null) {
            values = sampleValues();
        } else {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Student not found: " + studentId));
            values = valuesFor(student, summaryFor(student), "PREVIEW-00000", "PREV-IEWX-0000");
        }

        return fill(loadDesign(template), values);
    }

    /** An uploaded design, read from storage once and reused across a cohort. */
    private record LoadedDesign(byte[] bytes, String kind, String fileName,
                                List<FieldPlacement> fields) {
    }

    /** @return the design to fill, or null when the template renders as HTML */
    private LoadedDesign loadDesign(CertificateTemplate template) {
        if (template.getRenderMode() != RenderMode.OVERLAY) {
            return null;
        }
        requireOverlayReady(template);
        return new LoadedDesign(
                minioService.getAssetBytes(template.getAssetObjectName()),
                template.getAssetKind(),
                template.getAssetOriginalName(),
                readFields(template.getFieldsJson()));
    }

    /** Fills an uploaded design for one student and returns a PDF. */
    private byte[] fill(LoadedDesign design, Map<String, String> values) {
        try {
            return "IMAGE".equals(design.kind())
                    ? composer.composeOnImage(design.bytes(), design.fileName(),
                            design.fields(), values)
                    : composer.composeOnPdf(design.bytes(), design.fields(), values);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not produce the certificate: " + e.getMessage(), e);
        }
    }

    private void requireOverlayReady(CertificateTemplate template) {
        if (template.getRenderMode() != RenderMode.OVERLAY
                || template.getAssetObjectName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This template has no uploaded design to fill.");
        }
        if (readFields(template.getFieldsJson()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No fields have been placed on this design yet.");
        }
    }

    private String writeFields(List<FieldPlacement> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store the field positions.", e);
        }
    }

    private List<FieldPlacement> readFields(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FieldPlacement>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Issuing                                                             */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional
    public IssueBatchResponse issueBatch(IssueBatchRequest request) {
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Program not found: " + request.programId()));

        CertificateTemplate template = activeTemplateFor(request.certificateType(), request.programId());

        List<StudentAcademicSummaryResponse> cohort = cohortService.getCohort(
                request.programId(), request.yearLevel(), null, request.academicYear(), null);

        if (request.studentIds() != null && !request.studentIds().isEmpty()) {
            Set<UUID> wanted = new HashSet<>(request.studentIds());
            cohort = cohort.stream().filter(s -> wanted.contains(s.studentId())).toList();
        }

        if (cohort.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No students match that programme and year.");
        }

        Set<UUID> alreadyIssued = new HashSet<>(issuedRepository.findAlreadyIssuedStudentIds(
                request.certificateType(),
                cohort.stream().map(StudentAcademicSummaryResponse::studentId).toList()));

        // Loaded once, not per student: a 300-strong cohort would otherwise pull
        // the same design out of storage 300 times.
        LoadedDesign design = loadDesign(template);

        List<IssuedCertificateResponse> issued = new ArrayList<>();
        List<IssueBatchResponse.Skipped> skipped = new ArrayList<>();
        String actor = currentActor();
        LocalDateTime now = LocalDateTime.now();

        for (StudentAcademicSummaryResponse summary : cohort) {
            // Re-running a batch has to be safe: someone who already holds this
            // certificate is left alone rather than given a second one.
            if (alreadyIssued.contains(summary.studentId())) {
                skipped.add(new IssueBatchResponse.Skipped(summary.studentCode(),
                        summary.fullName(), "Already holds this certificate"));
                continue;
            }

            String problem = eligibility.problemFor(request.certificateType(), summary);
            if (problem != null && !request.shouldOverride()) {
                skipped.add(new IssueBatchResponse.Skipped(summary.studentCode(),
                        summary.fullName(), problem));
                continue;
            }

            Student student = studentRepository.findById(summary.studentId()).orElse(null);
            if (student == null) {
                continue;
            }

            issued.add(toIssuedResponse(award(
                    student, summary, program, template, design,
                    request.certificateType(), request.yearLevel(), request.academicYear(),
                    actor, now)));
        }

        return new IssueBatchResponse(issued.size(), skipped.size(), skipped, issued);
    }

    /**
     * The one path a certificate is created by.
     *
     * <p>A batch and a single approved request both come through here, so a
     * requested certificate carries the same number series, verification code
     * and template version as one awarded to a whole cohort. Two ways of
     * minting a certificate would mean two levels of trust in them.
     */
    private IssuedCertificate award(Student student, StudentAcademicSummaryResponse summary,
                                    Program program, CertificateTemplate template,
                                    LoadedDesign design, CertificateType type,
                                    Integer yearLevel, String academicYear,
                                    String actor, LocalDateTime now) {
        String number = numbers.nextNumber(type);
        String code = numbers.newVerificationCode();

        IssuedCertificate certificate = new IssuedCertificate();
        certificate.setStudent(student);
        certificate.setCertificateType(type);
        certificate.setCertificateNumber(number);
        certificate.setVerificationCode(code);
        certificate.setTemplate(template);
        certificate.setTemplateVersion(template.getVersion());
        certificate.setProgram(program);
        certificate.setYearLevel(yearLevel != null
                ? yearLevel : (summary != null ? summary.yearLevel() : null));
        certificate.setAcademicYear(academicYear != null
                ? academicYear : (summary != null ? summary.academicYear() : null));
        certificate.setStatus(IssuedStatus.ISSUED);
        certificate.setIssuedAt(now);
        certificate.setIssuedBy(actor);

        // Rendered once, here, and stored. Re-rendering on each download would
        // let a later grade correction silently change a certificate that is
        // already in the graduate's hands.
        Map<String, String> values = valuesFor(student, summary, number, code);

        if (design != null) {
            byte[] pdf = fill(design, values);
            certificate.setFileObjectName(minioService.uploadAssetBytes(
                    pdf, number + ".pdf", "application/pdf"));
            certificate.setFileOriginalName(number + ".pdf");
        } else {
            certificate.setRenderedHtml(renderer.render(template.getBodyHtml(), values));
        }

        IssuedCertificate saved = issuedRepository.save(certificate);
        notifyStudent(student, saved);
        return saved;
    }

    @Override
    @Transactional
    public IssuedCertificateResponse issueOne(UUID studentId, CertificateType type,
                                              boolean override) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found: " + studentId));

        Program program = student.getProgram();
        if (program == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This student is not enrolled on a programme, so there is no "
                            + "template to print from.");
        }

        if (!issuedRepository.findAlreadyIssuedStudentIds(type, List.of(studentId)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This student already holds that certificate.");
        }

        StudentAcademicSummaryResponse summary = summaryFor(student);
        String problem = eligibility.problemFor(type, summary);
        if (problem != null && !override) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem);
        }

        CertificateTemplate template = activeTemplateFor(type, program.getId());

        return toIssuedResponse(award(
                student, summary, program, template, loadDesign(template),
                type, student.getYearLevel(), student.getAcademicYear(),
                currentActor(), LocalDateTime.now()));
    }


    @Override
    @Transactional
    public IssuedCertificateResponse attachFile(UUID issuedId, MultipartFile file) {
        IssuedCertificate certificate = requireIssued(issuedId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded.");
        }

        certificate.setFileObjectName(minioService.uploadAsset(file));
        certificate.setFileOriginalName(file.getOriginalFilename());
        return toIssuedResponse(issuedRepository.save(certificate));
    }

    @Override
    public List<IssuedCertificateResponse> listIssued(CertificateType type, UUID programId) {
        return issuedRepository.search(type, programId).stream()
                .map(this::toIssuedResponse).toList();
    }

    @Override
    @Transactional
    public IssuedCertificateResponse revoke(UUID issuedId, RevokeCertificateRequest request) {
        IssuedCertificate certificate = requireIssued(issuedId);

        // Revoked rather than deleted, so verification can answer "withdrawn"
        // instead of "unknown" — a much stronger statement to whoever is asking.
        certificate.setStatus(IssuedStatus.REVOKED);
        certificate.setRevokedAt(LocalDateTime.now());
        certificate.setRevokeReason(request.reason().trim());

        return toIssuedResponse(issuedRepository.save(certificate));
    }

    @Override
    public VerificationResponse verify(String verificationCode) {
        if (verificationCode == null || verificationCode.isBlank()) {
            return VerificationResponse.notFound();
        }

        return issuedRepository
                .findByVerificationCode(verificationCode.trim().toUpperCase(Locale.ROOT))
                .map(c -> new VerificationResponse(
                        c.isValid(),
                        c.isValid()
                                ? "This certificate is genuine."
                                : "This certificate was revoked on "
                                        + c.getRevokedAt().toLocalDate() + ".",
                        c.getStudent() != null && c.getStudent().getUser() != null
                                ? c.getStudent().getUser().getFullName() : null,
                        c.getCertificateType(),
                        c.getCertificateNumber(),
                        c.getProgram() != null ? c.getProgram().getProgramName() : null,
                        c.getIssuedAt(),
                        !c.isValid()))
                .orElseGet(VerificationResponse::notFound);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private void notifyStudent(Student student, IssuedCertificate certificate) {
        if (student.getUser() == null) {
            return;
        }
        String label = certificate.getCertificateType().name()
                .replace("_", " ").toLowerCase(Locale.ROOT);

        notificationService.createNotification(
                student.getUser().getId(),
                "Your certificate is ready",
                "Your " + label + " certificate (" + certificate.getCertificateNumber()
                        + ") has been issued and can be downloaded now.",
                "CERTIFICATE",
                "Official Certificate",
                "Academic Registrar",
                "/certificates",
                "CERTIFICATE",
                certificate.getIssuedId());
    }

    private CertificateTemplate activeTemplateFor(CertificateType type, UUID programId) {
        List<CertificateTemplate> candidates = templateRepository.findActiveFor(type, programId);
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No active template for " + type
                            + ". Upload one and activate it before issuing.");
        }
        return candidates.getFirst();
    }

    private StudentAcademicSummaryResponse summaryFor(Student student) {
        UUID programId = student.getProgram() != null ? student.getProgram().getId() : null;
        return cohortService.getCohort(programId, null, null, null, null).stream()
                .filter(s -> s.studentId().equals(student.getStudentId()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> valuesFor(Student student, StudentAcademicSummaryResponse summary,
                                          String number, String code) {
        Map<String, String> values = new HashMap<>();

        values.put("studentName", student.getUser() != null && student.getUser().getFullName() != null
                ? student.getUser().getFullName() : "");
        values.put("studentCode", nullSafe(student.getStudentCode()));
        values.put("programName", student.getProgram() != null
                ? nullSafe(student.getProgram().getProgramName()) : "");
        values.put("degreeLevel", student.getProgram() != null
                ? nullSafe(student.getProgram().getDegreeLevel()) : "");
        values.put("yearLevel", student.getYearLevel() != null
                ? String.valueOf(student.getYearLevel()) : "");
        values.put("academicYear", nullSafe(student.getAcademicYear()));

        values.put("cumulativeGpa", summary != null && summary.cumulativeGpa() != null
                ? "%.2f".formatted(summary.cumulativeGpa()) : "");
        values.put("creditsEarned", summary != null && summary.creditsEarned() != null
                ? "%.0f".formatted(summary.creditsEarned()) : "");

        values.put("certificateNumber", number);
        values.put("verificationCode", code);
        values.put("verificationUrl", publicUrl + "/verify/" + code);
        values.put("issueDate", LocalDateTime.now().format(ISSUE_DATE));
        values.put("universityName", universityName);

        return values;
    }

    /** Stand-in values, so a template can be previewed before anyone is picked. */
    private Map<String, String> sampleValues() {
        Map<String, String> values = new HashMap<>();
        values.put("studentName", "Sok Dara");
        values.put("studentCode", "STU-001");
        values.put("programName", "Bachelor of Information Technology");
        values.put("degreeLevel", "Bachelor");
        values.put("yearLevel", "4");
        values.put("academicYear", "2025-2026");
        values.put("cumulativeGpa", "3.42");
        values.put("creditsEarned", "120");
        values.put("certificateNumber", "UT-2026-DEGREE-00042");
        values.put("verificationCode", "SAMP-LE00-CODE");
        values.put("verificationUrl", publicUrl + "/verify/SAMP-LE00-CODE");
        values.put("issueDate", LocalDateTime.now().format(ISSUE_DATE));
        values.put("universityName", universityName);
        return values;
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private void apply(CertificateTemplate template, SaveTemplateRequest request) {
        template.setCertificateType(request.certificateType());
        template.setName(request.name().trim());
        template.setBodyHtml(request.bodyHtml());
        template.setDescription(request.description());
        template.setProgram(request.programId() != null
                ? programRepository.findById(request.programId()).orElse(null)
                : null);
    }

    private CertificateTemplate requireTemplate(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Template not found: " + templateId));
    }

    private IssuedCertificate requireIssued(UUID issuedId) {
        return issuedRepository.findById(issuedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Certificate not found: " + issuedId));
    }

    private TemplateResponse toTemplateResponse(CertificateTemplate t) {
        return new TemplateResponse(
                t.getTemplateId(),
                t.getCertificateType(),
                t.getName(),
                t.getBodyHtml(),
                t.getProgram() != null ? t.getProgram().getId() : null,
                t.getProgram() != null ? t.getProgram().getProgramName() : null,
                t.getVersion(),
                t.getStatus(),
                t.getDescription(),
                t.getLastUpdateAt(),
                renderer.unknownPlaceholders(t.getBodyHtml()),
                t.getRenderMode(),
                t.getAssetKind(),
                t.getAssetOriginalName(),
                t.getAssetWidth(),
                t.getAssetHeight(),
                readFields(t.getFieldsJson()));
    }

    private IssuedCertificateResponse toIssuedResponse(IssuedCertificate c) {
        Student student = c.getStudent();
        return new IssuedCertificateResponse(
                c.getIssuedId(),
                student != null ? student.getStudentId() : null,
                student != null ? student.getStudentCode() : null,
                student != null && student.getUser() != null ? student.getUser().getFullName() : null,
                c.getCertificateType(),
                c.getCertificateNumber(),
                c.getVerificationCode(),
                c.getProgram() != null ? c.getProgram().getId() : null,
                c.getProgram() != null ? c.getProgram().getProgramName() : null,
                c.getYearLevel(),
                c.getAcademicYear(),
                c.getStatus(),
                c.getIssuedAt(),
                c.getIssuedBy(),
                c.getRevokedAt(),
                c.getRevokeReason(),
                c.getFileObjectName() != null,
                c.getRenderedHtml() != null);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
