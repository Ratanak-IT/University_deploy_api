package com.universitymanagement.grading.service;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.dto.response.CourseGradeResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read side of the gradebook, for transcripts, GPA and the cohort record sheet.
 * Everything here loads in a bounded number of queries no matter how many
 * students or courses are involved.
 */
public interface CourseGradeQueryService {

    /** One student's grade in each of the given course offerings. */
    List<CourseGradeResponse> gradesFor(UUID studentId, List<Classroom> classrooms);

    /** Grades for every student in the given offerings, keyed by student id. */
    Map<UUID, List<CourseGradeResponse>> gradesFor(List<Classroom> classrooms);
}
