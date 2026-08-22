package com.universitymanagement.subject.dto.response;

import java.util.UUID;

public record SubjectResponse(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        Double credit,
        Integer hours,
        UUID departmentId,

        /** Live classrooms teaching this subject. */
        Integer classroomCount
) {
    public SubjectResponse(UUID subjectId, String subjectCode, String subjectName, Double credit,
                           Integer hours, UUID departmentId) {
        this(subjectId, subjectCode, subjectName, credit, hours, departmentId, 0);
    }

    public SubjectResponse(UUID subjectId, String subjectCode, String subjectName, Double credit, UUID departmentId) {
        this(subjectId, subjectCode, subjectName, credit, null, departmentId, 0);
    }

    public SubjectResponse(String subjectCode, String subjectName, Double credit, UUID departmentId) {
        this(null, subjectCode, subjectName, credit, null, departmentId, 0);
    }

    public SubjectResponse(String subjectCode, String subjectName, Double credit, Integer hours, UUID departmentId) {
        this(null, subjectCode, subjectName, credit, hours, departmentId, 0);
    }

    public SubjectResponse withClassroomCount(int count) {
        return new SubjectResponse(subjectId, subjectCode, subjectName, credit, hours,
                departmentId, count);
    }

    public UUID getId() {
        return subjectId;
    }

    public UUID id() {
        return subjectId;
    }
}
