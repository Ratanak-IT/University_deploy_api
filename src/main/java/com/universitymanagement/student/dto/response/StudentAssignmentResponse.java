package com.universitymanagement.student.dto.response;

import com.universitymanagement.assignment.dto.response.FileResponse;
import com.universitymanagement.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Assignment as seen by a student, including their own submission state. */
public record StudentAssignmentResponse(
        UUID assignmentId,
        UUID classroomId,
        String className,
        String subjectName,
        String title,
        String description,
        LocalDateTime dueDate,
        Double maxScore,
        Double weight,
        List<FileResponse> assignmentFiles,
        // --- the student's own submission (null if not submitted yet) ---
        UUID submissionId,
        SubmissionStatus submissionStatus,
        LocalDateTime submittedAt,
        Double score,
        String feedback,
        LocalDateTime gradedAt,
        List<FileResponse> submissionFiles
) {
}
