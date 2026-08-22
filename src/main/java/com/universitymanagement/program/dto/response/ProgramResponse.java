package com.universitymanagement.program.dto.response;

import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String programName,
        String degreeLevel,
        Integer durationYears,
        UUID departmentId,
        String departmentName,

        /** Distinct subjects in this program's curriculum. */
        Integer subjectCount,
        /** Students currently enrolled in this program. */
        Integer studentCount
) {
    /** Used where the counts are not loaded; they read as zero rather than null. */
    public ProgramResponse(UUID id, String programName, String degreeLevel, Integer durationYears,
                           UUID departmentId, String departmentName) {
        this(id, programName, degreeLevel, durationYears, departmentId, departmentName, 0, 0);
    }

    public ProgramResponse withCounts(int subjects, int students) {
        return new ProgramResponse(id, programName, degreeLevel, durationYears,
                departmentId, departmentName, subjects, students);
    }
}
