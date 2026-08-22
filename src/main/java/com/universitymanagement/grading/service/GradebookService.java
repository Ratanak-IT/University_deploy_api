package com.universitymanagement.grading.service;

import com.universitymanagement.grading.dto.request.AssessmentRequest;
import com.universitymanagement.grading.dto.response.AssessmentResponse;
import com.universitymanagement.grading.dto.request.SaveScoresRequest;
import com.universitymanagement.grading.dto.response.GradebookResponse;

import java.util.UUID;

public interface GradebookService {

    /** The full marking grid, with every student's standing recalculated. */
    GradebookResponse getGradebook(UUID classroomId);

    /** Writes a batch of cells, then recalculates the affected students. */
    GradebookResponse saveScores(UUID classroomId, SaveScoresRequest request);

    AssessmentResponse createAssessment(UUID classroomId, AssessmentRequest request);

    AssessmentResponse updateAssessment(UUID classroomId, UUID assessmentId, AssessmentRequest request);

    void deleteAssessment(UUID classroomId, UUID assessmentId);

    /** Teacher signs off: grades freeze against further score edits. */
    GradebookResponse submit(UUID classroomId);

    /** Registrar posts: grades become transcript-visible and immutable. */
    GradebookResponse post(UUID classroomId);

    /** Admin-only escape hatch for a grade that was posted in error. */
    GradebookResponse reopen(UUID classroomId);
}
