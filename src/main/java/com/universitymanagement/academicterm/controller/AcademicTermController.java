package com.universitymanagement.academicterm.controller;

import com.universitymanagement.academicterm.dto.request.AcademicTermRequest;
import com.universitymanagement.academicterm.dto.response.AcademicTermResponse;
import com.universitymanagement.academicterm.service.AcademicTermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-terms")
@RequiredArgsConstructor
public class AcademicTermController {

    private final AcademicTermService academicTermService;

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public AcademicTermResponse create(@Valid @RequestBody AcademicTermRequest request) {
        return academicTermService.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{termId}")
    public AcademicTermResponse update(@PathVariable UUID termId, @Valid @RequestBody AcademicTermRequest request) {
        return academicTermService.update(termId, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping
    public List<AcademicTermResponse> getAll() {
        return academicTermService.getAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/{termId}")
    public AcademicTermResponse getById(@PathVariable UUID termId) {
        return academicTermService.getById(termId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{termId}")
    public void delete(@PathVariable UUID termId) {
        academicTermService.delete(termId);
    }
}
