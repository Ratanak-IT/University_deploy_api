package com.universitymanagement.student.dto.response;

import java.util.UUID;

/** Weighted grade for one classroom (subject) of the student. */
public record GradeResponse(
        UUID classroomId,
        String className,
        UUID subjectId,
        String subjectCode,
        String subjectName,
        Double credit,
        String academicYear,
        Integer semester,
        Integer gradedAssignments,
        Integer totalAssignments,
        Double scorePercent,
        String letterGrade,
        Double gradePoint
) {
}
