package com.universitymanagement.grading.controller;

import com.universitymanagement.grading.dto.request.AssessmentRequest;
import com.universitymanagement.grading.dto.request.SaveGradeSchemeRequest;
import com.universitymanagement.grading.dto.request.SaveScoresRequest;
import com.universitymanagement.grading.dto.response.AssessmentResponse;
import com.universitymanagement.grading.dto.response.GradeComponentResponse;
import com.universitymanagement.grading.dto.response.GradebookResponse;
import com.universitymanagement.grading.service.GradeSchemeService;
import com.universitymanagement.grading.service.GradebookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}")
@RequiredArgsConstructor
public class GradebookController {

    private final GradebookService gradebookService;
    private final GradeSchemeService schemeService;

    // ---- grading policy ----

    @GetMapping("/grade-scheme")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<GradeComponentResponse> getScheme(@PathVariable UUID classroomId) {
        return schemeService.getScheme(classroomId);
    }

    @PutMapping("/grade-scheme")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<GradeComponentResponse> saveScheme(@PathVariable UUID classroomId,
                                                   @Valid @RequestBody SaveGradeSchemeRequest request) {
        return schemeService.saveScheme(classroomId, request);
    }

    // ---- the grid ----

    @GetMapping("/gradebook")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GradebookResponse getGradebook(@PathVariable UUID classroomId) {
        return gradebookService.getGradebook(classroomId);
    }

    @PostMapping("/gradebook/scores")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GradebookResponse saveScores(@PathVariable UUID classroomId,
                                        @Valid @RequestBody SaveScoresRequest request) {
        return gradebookService.saveScores(classroomId, request);
    }

    // ---- assessment columns ----

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public AssessmentResponse createAssessment(@PathVariable UUID classroomId,
                                               @Valid @RequestBody AssessmentRequest request) {
        return gradebookService.createAssessment(classroomId, request);
    }

    @PutMapping("/assessments/{assessmentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public AssessmentResponse updateAssessment(@PathVariable UUID classroomId,
                                               @PathVariable UUID assessmentId,
                                               @Valid @RequestBody AssessmentRequest request) {
        return gradebookService.updateAssessment(classroomId, assessmentId, request);
    }

    @DeleteMapping("/assessments/{assessmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public void deleteAssessment(@PathVariable UUID classroomId,
                                 @PathVariable UUID assessmentId) {
        gradebookService.deleteAssessment(classroomId, assessmentId);
    }

    // ---- lifecycle ----

    @PostMapping("/gradebook/submit")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GradebookResponse submit(@PathVariable UUID classroomId) {
        return gradebookService.submit(classroomId);
    }

    @PostMapping("/gradebook/post")
    @PreAuthorize("hasRole('ADMIN')")
    public GradebookResponse post(@PathVariable UUID classroomId) {
        return gradebookService.post(classroomId);
    }

    @PostMapping("/gradebook/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public GradebookResponse reopen(@PathVariable UUID classroomId) {
        return gradebookService.reopen(classroomId);
    }
}
