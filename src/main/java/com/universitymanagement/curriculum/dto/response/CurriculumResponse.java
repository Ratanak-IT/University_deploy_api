package com.universitymanagement.curriculum.dto.response;

import com.universitymanagement.curriculum.entity.CourseType;
import java.util.UUID;

public record CurriculumResponse(
        UUID curriculumId,
        Integer semester,
        Integer yearLevel,
        UUID programId,
        String programName,
        UUID subjectId,
        String subjectName,
        String subjectCode,
        Double credit,
        CourseType courseType,
        UUID prerequisiteSubjectId,
        String prerequisiteSubjectName,
        String prerequisiteSubjectCode,
        Integer lectureHours,
        Integer labHours
) {
}
