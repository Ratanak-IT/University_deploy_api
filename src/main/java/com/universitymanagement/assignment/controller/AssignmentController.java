package com.universitymanagement.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitymanagement.assignment.dto.request.AssignmentRequest;
import com.universitymanagement.assignment.dto.request.GradeSubmissionRequest;
import com.universitymanagement.assignment.dto.response.AssignmentResponse;
import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import com.universitymanagement.assignment.exception.InvalidAssignmentPayloadException;
import com.universitymanagement.assignment.service.AssignmentService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(
            value = "/classrooms/{classroomId}/assignments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public AssignmentResponse createAssignment(
            @PathVariable UUID classroomId,
            @RequestPart("assignment") String assignmentJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        AssignmentRequest request = parseAndValidate(assignmentJson);
        return assignmentService.createAssignment(classroomId, request, files);
    }

    private AssignmentRequest parseAndValidate(String assignmentJson) {
        AssignmentRequest request;
        try {
            request = objectMapper.readValue(assignmentJson, AssignmentRequest.class);
        } catch (Exception e) {
            throw new InvalidAssignmentPayloadException(
                    "\"assignment\" part is not valid JSON: " + e.getMessage());
        }

        Set<ConstraintViolation<AssignmentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new InvalidAssignmentPayloadException(detail);
        }
        return request;
    }

    @GetMapping("/classrooms/{classroomId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public List<AssignmentResponse> getAssignmentsByClassroom(@PathVariable UUID classroomId) {
        return assignmentService.getAssignmentsByClassroom(classroomId);
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<SubmissionResponse> getSubmissions(@PathVariable UUID assignmentId) {
        return assignmentService.getSubmissionsByAssignment(assignmentId);
    }

    @PostMapping(
            value = "/assignments/{assignmentId}/submissions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionResponse submitAssignment(
            @PathVariable UUID assignmentId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return assignmentService.submitAssignment(assignmentId, files);
    }

    @PatchMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public SubmissionResponse gradeSubmission(
            @PathVariable UUID submissionId,
            @Valid @RequestBody GradeSubmissionRequest request
    ) {
        return assignmentService.gradeSubmission(submissionId, request);
    }
}