package com.universitymanagement.student.controller;

import com.universitymanagement.grading.entity.AcademicStanding;
import com.universitymanagement.student.dto.response.AcademicRecordSheetResponse;
import com.universitymanagement.student.dto.response.StudentAcademicSummaryResponse;
import com.universitymanagement.student.service.StudentAcademicService;
import com.universitymanagement.student.service.StudentCohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-records")
@RequiredArgsConstructor
public class AcademicRecordSheetController {

    private final StudentAcademicService academicService;
    private final StudentCohortService cohortService;

    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/sheet")
    public AcademicRecordSheetResponse getSheet(
            @RequestParam(required = false) UUID programId,
            @RequestParam(required = false) Integer yearLevel,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String academicYear) {
        return academicService.getAcademicRecordSheet(programId, yearLevel, semester, academicYear);
    }

    /**
     * Cumulative standing for a whole cohort — the transcript list.
     *
     * <p>Distinct from {@code /sheet}, which is one term's subject grades laid
     * out as a matrix. This one spans every term a student has taken.
     */
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/students")
    public List<StudentAcademicSummaryResponse> getCohort(
            @RequestParam(required = false) UUID programId,
            @RequestParam(required = false) Integer yearLevel,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) AcademicStanding standing) {
        return cohortService.getCohort(programId, yearLevel, semester, academicYear, standing);
    }
}