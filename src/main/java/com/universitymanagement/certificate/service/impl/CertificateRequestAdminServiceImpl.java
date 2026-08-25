package com.universitymanagement.certificate.service.impl;

import com.universitymanagement.certificate.dto.request.RejectRequestRequest;
import com.universitymanagement.certificate.dto.response.AdminCertificateRequestResponse;
import com.universitymanagement.certificate.dto.response.IssuedCertificateResponse;
import com.universitymanagement.certificate.entity.CertificateRequest;
import com.universitymanagement.certificate.entity.CertificateStatus;
import com.universitymanagement.certificate.entity.CertificateTemplate;
import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.entity.IssuedCertificate;
import com.universitymanagement.certificate.repository.CertificateRequestRepository;
import com.universitymanagement.certificate.repository.CertificateTemplateRepository;
import com.universitymanagement.certificate.repository.IssuedCertificateRepository;
import com.universitymanagement.certificate.service.CertificateAdminService;
import com.universitymanagement.certificate.service.CertificateEligibility;
import com.universitymanagement.certificate.service.CertificateRequestAdminService;
import com.universitymanagement.notification.service.NotificationService;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.student.dto.response.StudentAcademicSummaryResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.service.StudentCohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The registrar's request inbox.
 *
 * <p>Nothing here creates a certificate. Approving calls the same issuing path
 * a cohort batch uses, so a certificate that came from a request carries the
 * same number series, verification code and template version as any other. The
 * alternative — attaching a file by hand on approval — produces a document that
 * cannot be verified or revoked, which is not really a certificate.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateRequestAdminServiceImpl implements CertificateRequestAdminService {

    private final CertificateRequestRepository requestRepository;
    private final CertificateTemplateRepository templateRepository;
    private final IssuedCertificateRepository issuedRepository;
    private final StudentCohortService cohortService;
    private final CertificateAdminService adminService;
    private final CertificateEligibility eligibility;
    private final NotificationService notificationService;

    @Override
    public List<AdminCertificateRequestResponse> list(CertificateStatus status,
                                                      CertificateType type,
                                                      UUID programId) {
        List<CertificateRequest> requests = requestRepository.search(status, type, programId);
        if (requests.isEmpty()) {
            return List.of();
        }

        Verdicts verdicts = verdictsFor(requests);

        // Pending first: those are the only rows that need a decision, and a
        // registrar opening this screen is here to make decisions.
        return requests.stream()
                .sorted((a, b) -> {
                    int byStatus = Integer.compare(rank(a.getStatus()), rank(b.getStatus()));
                    return byStatus != 0 ? byStatus : b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(r -> toResponse(r, verdicts))
                .toList();
    }

    private int rank(CertificateStatus status) {
        return status == CertificateStatus.PENDING ? 0 : 1;
    }

    @Override
    @Transactional
    public AdminCertificateRequestResponse approve(UUID requestId, boolean override) {
        CertificateRequest request = requirePending(requestId);
        Student student = request.getStudent();

        // Delegated, not duplicated. issueOne runs the eligibility check, picks
        // the active template, composes the PDF, allocates the number and
        // verification code, and notifies the student.
        IssuedCertificateResponse issued = adminService.issueOne(
                student.getStudentId(), request.getCertificateType(), override);

        IssuedCertificate certificate = issuedRepository.findById(issued.issuedId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The certificate was issued but could not be linked to the request."));

        request.setIssuedCertificate(certificate);
        request.setStatus(CertificateStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(currentActor());

        CertificateRequest saved = requestRepository.save(request);
        return toResponse(saved, Verdicts.none());
    }

    @Override
    @Transactional
    public AdminCertificateRequestResponse reject(UUID requestId, RejectRequestRequest body) {
        CertificateRequest request = requirePending(requestId);

        request.setStatus(CertificateStatus.REJECTED);
        request.setRejectReason(body.reason().trim());
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(currentActor());

        CertificateRequest saved = requestRepository.save(request);
        notifyRejected(saved);

        return toResponse(saved, Verdicts.none());
    }

    private CertificateRequest requirePending(UUID requestId) {
        CertificateRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Request not found: " + requestId));

        // Deciding twice would either issue a second certificate or overwrite a
        // recorded reason, so a settled request is left alone.
        if (request.getStatus() != CertificateStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This request was already "
                            + request.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }
        return request;
    }

    private void notifyRejected(CertificateRequest request) {
        Student student = request.getStudent();
        if (student.getUser() == null) {
            return;
        }
        String label = request.getCertificateType().name()
                .replace("_", " ").toLowerCase(Locale.ROOT);

        notificationService.createNotification(
                student.getUser().getId(),
                "Your certificate request was not approved",
                "Your request for a " + label + " certificate was declined: "
                        + request.getRejectReason(),
                "CERTIFICATE",
                "Official Certificate Application",
                "Academic Registrar",
                "/dashboard/student/certificates",
                "CERTIFICATE",
                request.getRequestId());
    }

    /* ------------------------------------------------------------------ */
    /* Working out, up front, what approving each request would do         */
    /* ------------------------------------------------------------------ */

    /**
     * The answers the list needs, gathered in bulk.
     *
     * @param summaries     academic summary per student
     * @param templateNames the active template per type and programme, keyed by
     *                      {@code type + "/" + programId}; absent means none
     * @param alreadyHeld   students who already hold that type, keyed the same
     */
    private record Verdicts(Map<UUID, StudentAcademicSummaryResponse> summaries,
                            Map<String, String> templateNames,
                            Set<String> alreadyHeld) {

        static Verdicts none() {
            return new Verdicts(Map.of(), Map.of(), Set.of());
        }
    }

    /**
     * One pass over the whole list rather than one lookup per row.
     *
     * <p>Working a verdict out per request would call the cohort service once
     * per student — and that call reads a whole programme. Fifty pending
     * requests would mean fifty full-programme reads.
     */
    private Verdicts verdictsFor(List<CertificateRequest> requests) {
        Map<UUID, StudentAcademicSummaryResponse> summaries = new HashMap<>();
        Map<String, String> templateNames = new HashMap<>();
        Set<String> alreadyHeld = new HashSet<>();

        Set<UUID> programIds = requests.stream()
                .map(r -> programIdOf(r.getStudent()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        for (UUID programId : programIds) {
            cohortService.getCohort(programId, null, null, null, null)
                    .forEach(s -> summaries.putIfAbsent(s.studentId(), s));
        }

        // One template lookup and one already-issued lookup per distinct
        // (type, programme) pair the inbox actually contains.
        Map<String, List<CertificateRequest>> grouped = requests.stream()
                .collect(Collectors.groupingBy(
                        r -> key(r.getCertificateType(), programIdOf(r.getStudent()))));

        grouped.forEach((key, group) -> {
            CertificateType type = group.getFirst().getCertificateType();
            UUID programId = programIdOf(group.getFirst().getStudent());

            List<CertificateTemplate> active = templateRepository.findActiveFor(type, programId);
            if (!active.isEmpty()) {
                templateNames.put(key, active.getFirst().getName());
            }

            List<UUID> studentIds = group.stream()
                    .map(r -> r.getStudent().getStudentId())
                    .toList();
            issuedRepository.findAlreadyIssuedStudentIds(type, studentIds)
                    .forEach(id -> alreadyHeld.add(key(type, programId) + "/" + id));
        });

        return new Verdicts(summaries, templateNames, alreadyHeld);
    }

    private String key(CertificateType type, UUID programId) {
        return type.name() + "/" + programId;
    }

    private UUID programIdOf(Student student) {
        Program program = student.getProgram();
        return program != null ? program.getId() : null;
    }

    private AdminCertificateRequestResponse toResponse(CertificateRequest request,
                                                       Verdicts verdicts) {
        Student student = request.getStudent();
        UUID programId = programIdOf(student);
        String key = key(request.getCertificateType(), programId);

        String blocked = null;
        boolean overridable = false;
        String templateName = verdicts.templateNames().get(key);

        if (request.getStatus() == CertificateStatus.PENDING) {
            if (programId == null) {
                blocked = "Not enrolled on a programme";
            } else if (verdicts.alreadyHeld().contains(
                    key + "/" + student.getStudentId())) {
                blocked = "Already holds this certificate";
            } else if (templateName == null) {
                blocked = "No active template for this certificate type";
            } else {
                // Only an eligibility rule may be overridden. A missing template
                // is not a judgement call — there is nothing to print.
                blocked = eligibility.problemFor(request.getCertificateType(),
                        verdicts.summaries().get(student.getStudentId()));
                overridable = blocked != null;
            }
        }

        IssuedCertificate issued = request.getIssuedCertificate();

        return new AdminCertificateRequestResponse(
                request.getRequestId(),
                student.getStudentId(),
                student.getStudentCode(),
                student.getUser() != null ? student.getUser().getFullName() : null,
                programId,
                student.getProgram() != null ? student.getProgram().getProgramName() : null,
                student.getYearLevel(),
                request.getCertificateType(),
                request.getReason(),
                request.getStatus(),
                request.getRejectReason(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getProcessedBy(),
                blocked,
                templateName,
                overridable,
                issued != null ? issued.getIssuedId() : null,
                issued != null ? issued.getCertificateNumber() : null,
                issued != null ? issued.getVerificationCode() : null);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
