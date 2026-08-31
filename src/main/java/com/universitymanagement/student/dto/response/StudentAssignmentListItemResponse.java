package com.universitymanagement.student.dto.response;

import com.universitymanagement.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of a student's assignment list — deliberately not
 * {@link StudentAssignmentResponse}: no description, no file list, so
 * nothing here costs a signed MinIO URL. The assignments list page only
 * renders a title, due date, class label and status chip; the detail page
 * (which does need files) fetches its own single assignment separately.
 */
public record StudentAssignmentListItemResponse(
        UUID assignmentId,
        UUID classroomId,
        String className,
        String subjectName,
        String title,
        LocalDateTime dueDate,
        Double maxScore,
        SubmissionStatus submissionStatus,
        Double score
) {
}
