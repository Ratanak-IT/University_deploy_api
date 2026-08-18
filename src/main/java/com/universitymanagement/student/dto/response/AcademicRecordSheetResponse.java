package com.universitymanagement.student.dto.response;

import java.util.List;
import java.util.UUID;


public record AcademicRecordSheetResponse(
        UUID programId,
        String programName,
        Integer yearLevel,
        Integer semester,
        String academicYear,
        List<SubjectColumn> subjects,
        List<StudentRow> students
) {
    public record SubjectColumn(UUID subjectId, String subjectCode, String subjectName) {
    }

    public record StudentRow(
            UUID studentId,
            String studentCode,
            String fullName,
            List<GradeCell> grades,
            Double gpa
    ) {
    }

    public record GradeCell(UUID subjectId, Double scorePercent, String letterGrade, Double gradePoint) {
    }
}