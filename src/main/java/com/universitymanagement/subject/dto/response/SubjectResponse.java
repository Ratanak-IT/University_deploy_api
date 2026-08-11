package com.universitymanagement.subject.dto.response;

import java.util.UUID;

public record SubjectResponse(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        Double credit,
        UUID departmentId
) {
    public SubjectResponse(String subjectCode, String subjectName, Double credit, UUID departmentId) {
        this(null, subjectCode, subjectName, credit, departmentId);
    }

    public UUID getId() {
        return subjectId;
    }

    public UUID id() {
        return subjectId;
    }
}
