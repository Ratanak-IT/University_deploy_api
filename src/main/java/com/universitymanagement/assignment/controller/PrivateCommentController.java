package com.universitymanagement.assignment.controller;

import com.universitymanagement.assignment.dto.request.CreatePrivateCommentRequest;
import com.universitymanagement.assignment.dto.response.PrivateCommentResponse;
import com.universitymanagement.assignment.service.PrivateCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentId}")
@RequiredArgsConstructor
public class PrivateCommentController {

    private final PrivateCommentService privateCommentService;

    /** A student reading their own private thread for this assignment. */
    @GetMapping("/private-comments")
    @PreAuthorize("hasRole('STUDENT')")
    public List<PrivateCommentResponse> getMyThread(@PathVariable UUID assignmentId) {
        return privateCommentService.getMyThread(assignmentId);
    }

    /** A student posting to their own private thread. */
    @PostMapping("/private-comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public PrivateCommentResponse postToMyThread(@PathVariable UUID assignmentId,
                                                 @Valid @RequestBody CreatePrivateCommentRequest request) {
        return privateCommentService.postToMyThread(assignmentId, request);
    }

    /** A teacher (or admin) reading one specific student's private thread. */
    @GetMapping("/students/{studentId}/private-comments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<PrivateCommentResponse> getStudentThread(@PathVariable UUID assignmentId,
                                                         @PathVariable UUID studentId) {
        return privateCommentService.getStudentThread(assignmentId, studentId);
    }

    /** A teacher (or admin) posting to one specific student's private thread. */
    @PostMapping("/students/{studentId}/private-comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public PrivateCommentResponse postToStudentThread(@PathVariable UUID assignmentId,
                                                       @PathVariable UUID studentId,
                                                       @Valid @RequestBody CreatePrivateCommentRequest request) {
        return privateCommentService.postToStudentThread(assignmentId, studentId, request);
    }
}
