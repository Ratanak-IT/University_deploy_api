package com.universitymanagement.curriculum.dto.response;

import java.util.List;
import java.util.UUID;

public record CurriculumStructureResponse(
        UUID programId,
        String programName,
        Double totalCredits,
        Double coreCredits,
        Double generalCredits,
        Double electiveCredits,
        Double thesisCredits,
        List<SemesterGroupResponse> semesters
) {
    public record SemesterGroupResponse(
            Integer yearLevel,
            Integer semester,
            Double semesterCredits,
            List<CurriculumResponse> subjects
    ) {}
}
