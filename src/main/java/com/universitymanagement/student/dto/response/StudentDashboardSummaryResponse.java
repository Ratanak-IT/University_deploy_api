package com.universitymanagement.student.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight numbers for the student dashboard's stat cards and deadlines
 * widget — deliberately not {@link StudentAssignmentResponse}, which carries
 * descriptions and signed file URLs the dashboard never renders.
 */
public record StudentDashboardSummaryResponse(
        long pendingAssignments,
        List<UpcomingDeadline> upcomingDeadlines
) {
    public record UpcomingDeadline(
            UUID assignmentId,
            String title,
            String classCode,
            LocalDateTime dueDate
    ) {
    }
}
