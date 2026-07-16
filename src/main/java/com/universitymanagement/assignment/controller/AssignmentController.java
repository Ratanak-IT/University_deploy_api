package com.universitymanagement.assignment.controller;

import com.universitymanagement.assignment.dto.request.AssignmentRequest;
import com.universitymanagement.assignment.dto.request.GradeSubmissionRequest;
import com.universitymanagement.assignment.dto.response.AssignmentResponse;
import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import com.universitymanagement.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping(
            value = "/classrooms/{classroomId}/assignments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public AssignmentResponse createAssignment(
            @PathVariable UUID classroomId,
            @Valid @RequestPart("assignment") AssignmentRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return assignmentService.createAssignment(classroomId, request, files);
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