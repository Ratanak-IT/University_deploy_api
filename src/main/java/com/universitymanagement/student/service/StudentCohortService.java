package com.universitymanagement.student.service;

import com.universitymanagement.grading.entity.AcademicStanding;
import com.universitymanagement.student.dto.response.StudentAcademicSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface StudentCohortService {

    /**
     * Cumulative academic standing for a cohort — the transcript list.
     *
     * @param standing keep only students in this standing, for the question
     *                 the screen actually exists to answer: who is in trouble
     */
    List<StudentAcademicSummaryResponse> getCohort(UUID programId, Integer yearLevel,
                                                   Integer semester, String academicYear,
                                                   AcademicStanding standing);
}
