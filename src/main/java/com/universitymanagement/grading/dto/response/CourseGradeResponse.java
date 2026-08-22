package com.universitymanagement.grading.dto.response;

import com.universitymanagement.grading.entity.CourseGradeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A student's grade in one course, as it appears on a transcript row. */
public record CourseGradeResponse(
        UUID courseGradeId,
        UUID studentId,
        String studentCode,
        String fullName,

        UUID classroomId,
        String className,
        UUID subjectId,
        String subjectCode,
        String subjectName,
        Double credit,
        String academicYear,
        Integer semester,

        Double scorePercent,
        String letterGrade,
        Double gradePoint,
        Double creditsEarned,
        Double completenessPercent,
        CourseGradeStatus status,
        Boolean countsInGpa,
        LocalDateTime postedAt,
        String remark,

        List<GradebookResponse.ComponentBreakdown> breakdown
) {
}
