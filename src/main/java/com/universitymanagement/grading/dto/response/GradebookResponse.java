package com.universitymanagement.grading.dto.response;

import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.CourseGradeStatus;
import com.universitymanagement.grading.entity.ScoreStatus;

import java.util.List;
import java.util.UUID;

/**
 * The whole marking grid for one classroom: the policy across the top, the
 * roster down the side, and every student's standing already calculated.
 */
public record GradebookResponse(
        UUID classroomId,
        String className,
        String subjectCode,
        String subjectName,
        Double credit,
        String academicYear,
        Integer semester,

        /** Sum of component weights. Anything but 100 means the policy is unfinished. */
        Double totalWeight,
        boolean schemeValid,

        /**
         * The least advanced status on the roster, so the gradebook only reads as
         * submitted or posted once every student has got there.
         */
        CourseGradeStatus status,
        boolean locked,

        List<GradeComponentResponse> components,
        List<StudentRow> students
) {
    public record StudentRow(
            UUID studentId,
            String studentCode,
            String fullName,
            /** Presigned preview URL, or null when the student has no photo. */
            String avatarUrl,
            List<Cell> cells,
            List<ComponentBreakdown> breakdown,
            Double scorePercent,
            Double completenessPercent,
            String letterGrade,
            Double gradePoint,
            CourseGradeStatus status
    ) {
    }

    public record Cell(
            UUID assessmentId,
            Double score,
            ScoreStatus status
    ) {
    }

    public record ComponentBreakdown(
            UUID componentId,
            String name,
            ComponentSource source,
            Double weightPercent,
            /** Null when nothing in this component has been marked yet. */
            Double percent,
            Double earnedPoints,
            Double possiblePoints,
            Integer gradedItems,
            Integer totalItems
    ) {
    }
}
