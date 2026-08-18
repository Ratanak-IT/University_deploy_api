package com.universitymanagement.student.controller;

import com.universitymanagement.student.dto.response.AcademicRecordSheetResponse;
import com.universitymanagement.student.service.StudentAcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-records")
@RequiredArgsConstructor
public class AcademicRecordSheetController {

    private final StudentAcademicService academicService;

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
}