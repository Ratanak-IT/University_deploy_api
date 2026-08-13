package com.universitymanagement.subject.dto.response;

import java.util.UUID;

public record SubjectResponse(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        Double credit,
        Integer hours,
        UUID departmentId
) {
    public SubjectResponse(UUID subjectId, String subjectCode, String subjectName, Double credit, UUID departmentId) {
        this(subjectId, subjectCode, subjectName, credit, null, departmentId);
    }

    public SubjectResponse(String subjectCode, String subjectName, Double credit, UUID departmentId) {
        this(null, subjectCode, subjectName, credit, null, departmentId);
    }

    public SubjectResponse(String subjectCode, String subjectName, Double credit, Integer hours, UUID departmentId) {
        this(null, subjectCode, subjectName, credit, hours, departmentId);
    }

    public UUID getId() {
        return subjectId;
    }

    public UUID id() {
        return subjectId;
    }
}
