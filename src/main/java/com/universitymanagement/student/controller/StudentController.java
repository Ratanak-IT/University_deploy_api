package com.universitymanagement.student.controller;

import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import com.universitymanagement.assignment.service.AssignmentService;
import com.universitymanagement.attendance.dto.response.StudentAttendanceResponse;
import com.universitymanagement.attendance.dto.response.StudentTimetableSlotResponse;
import com.universitymanagement.certificate.dto.request.CreateCertificateRequest;
import com.universitymanagement.certificate.dto.response.CertificateDownloadResponse;
import com.universitymanagement.certificate.dto.response.CertificateRequestResponse;
import com.universitymanagement.certificate.dto.response.IssuedCertificateResponse;
import com.universitymanagement.certificate.service.StudentCertificateService;
import com.universitymanagement.certificate.service.CertificateService;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.grading.dto.response.CourseGradeResponse;
import com.universitymanagement.quiz.dto.request.SubmitQuizAttemptRequest;
import com.universitymanagement.quiz.dto.response.QuizAttemptResponse;
import com.universitymanagement.quiz.dto.response.QuizResponse;
import com.universitymanagement.quiz.service.QuizAttemptService;
import com.universitymanagement.student.dto.request.StudentUpdateProfileRequest;
import com.universitymanagement.student.dto.response.*;
import com.universitymanagement.student.security.StudentAccessGuard;
import com.universitymanagement.student.service.StudentAcademicService;
import com.universitymanagement.student.service.StudentService;
import com.universitymanagement.subject.dto.response.SubjectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentAcademicService academicService;
    private final CertificateService certificateService;
    private final StudentCertificateService studentCertificateService;
    private final QuizAttemptService quizAttemptService;
    private final AssignmentService assignmentService;
    private final StudentAccessGuard accessGuard;

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public StudentDetailResponse getMyProfile() {
        return studentService.getMyProfile();
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/me")
    public StudentDetailResponse updateMyProfile(
            @Valid @RequestBody StudentUpdateProfileRequest request) {
        return studentService.updateMyProfile(request);
    }
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentDetailResponse uploadMyAvatar(@RequestPart("file") MultipartFile file) {
        return studentService.uploadMyAvatar(file);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/transcript")
    public TranscriptResponse getTranscript(@PathVariable UUID studentId) {
        return academicService.getTranscript(studentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/grades")
    public List<CourseGradeResponse> getGrades(@PathVariable UUID studentId) {
        return academicService.getGrades(studentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/gpa")
    public GpaResponse getGpa(@PathVariable UUID studentId) {
        return academicService.getGpa(studentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/attendance")
    public List<StudentAttendanceResponse> getAttendance(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID classroomId) {
        return academicService.getAttendance(studentId, classroomId);
    }

    /** The weekly timetable across every classroom the student is enrolled in. */
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/timetable")
    public List<StudentTimetableSlotResponse> getTimetable(@PathVariable UUID studentId) {
        return academicService.getTimetable(studentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/department")
    public List<DepartmentResponse> getDepartments(@PathVariable UUID studentId) {
        return academicService.getDepartments(studentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/subjects")
    public List<SubjectResponse> getSubjects(@PathVariable UUID studentId) {
        return academicService.getSubjects(studentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/assignments")
    public Page<StudentAssignmentResponse> getAssignments(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return academicService.getAssignments(studentId, subjectId, status, page, size);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/assignments/{assignmentId}")
    public StudentAssignmentResponse getAssignmentDetail(@PathVariable UUID studentId,
                                                         @PathVariable UUID assignmentId) {
        return academicService.getAssignmentDetail(studentId, assignmentId);
    }

    /**
     * Certificates the student has actually been awarded.
     *
     * <p>Nothing is listed until the registrar issues it — there is no row to
     * read before then, so an unapproved certificate cannot be seen or
     * downloaded by any route.
     */
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/certificates")
    public List<IssuedCertificateResponse> getMyCertificates(@PathVariable UUID studentId) {
        return studentCertificateService.getMyCertificates(studentId);
    }

    /** The awarded document itself, as stored at the moment it was issued. */
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping(value = "/{studentId}/certificates/{issuedId}/document",
            produces = MediaType.TEXT_HTML_VALUE)
    public String getCertificateDocument(@PathVariable UUID studentId,
                                         @PathVariable UUID issuedId) {
        return studentCertificateService.getCertificateHtml(studentId, issuedId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/certificates/{issuedId}/download")
    public CertificateDownloadResponse downloadIssuedCertificate(@PathVariable UUID studentId,
                                                                 @PathVariable UUID issuedId) {
        return studentCertificateService.download(studentId, issuedId);
    }

    /** Same file, opened inline so it can be looked at before it is saved. */
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/certificates/{issuedId}/preview")
    public CertificateDownloadResponse previewIssuedCertificate(@PathVariable UUID studentId,
                                                                 @PathVariable UUID issuedId) {
        return studentCertificateService.preview(studentId, issuedId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/certificate-requests")
    public List<CertificateRequestResponse> getCertificateRequests(@PathVariable UUID studentId) {
        return certificateService.getRequestsForStudent(studentId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{studentId}/certificate-requests")
    public CertificateRequestResponse createCertificateRequest(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateCertificateRequest request) {
        return certificateService.createRequest(studentId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/certificate-requests/{requestId}/download")
    public CertificateDownloadResponse downloadCertificate(@PathVariable UUID studentId,
                                                           @PathVariable UUID requestId) {
        return certificateService.downloadApprovedCertificate(studentId, requestId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/quizzes")
    public List<QuizResponse> getQuizzes(@PathVariable UUID studentId) {
        return quizAttemptService.getQuizzesForStudent(studentId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{studentId}/quizzes/{quizId}/attempts")
    public QuizAttemptResponse startQuizAttempt(@PathVariable UUID studentId,
                                                @PathVariable UUID quizId) {
        return quizAttemptService.startAttempt(studentId, quizId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/{studentId}/quizzes/{quizId}/attempts/{attemptId}")
    public QuizAttemptResponse submitQuizAttempt(
            @PathVariable UUID studentId,
            @PathVariable UUID quizId,
            @PathVariable UUID attemptId,
            @Valid @RequestBody SubmitQuizAttemptRequest request) {
        return quizAttemptService.submitAttempt(studentId, quizId, attemptId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TEACHER')")
    @GetMapping("/{studentId}/quizzes/{quizId}/attempts/{attemptId}")
    public QuizAttemptResponse getQuizAttemptResult(@PathVariable UUID studentId,
                                                    @PathVariable UUID quizId,
                                                    @PathVariable UUID attemptId) {
        return quizAttemptService.getAttemptResult(studentId, quizId, attemptId);
    }
}
