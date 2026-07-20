package com.universitymanagement.program.controller;

import com.universitymanagement.program.dto.request.ProgramRequest;
import com.universitymanagement.program.dto.response.ProgramResponse;
import com.universitymanagement.program.service.ProgramService;
import com.universitymanagement.student.dto.response.StudentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
public class ProgramController {
    private final ProgramService programService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse create(@Valid @RequestBody ProgramRequest request){
        return programService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ProgramResponse getById(@PathVariable UUID id) {
        return programService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public Page<ProgramResponse> getAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "25") Integer size){
        return programService.getAll(page, size);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramRequest request) {

        return programService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        programService.delete(id);
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<StudentResponse> getStudentsByProgram(@PathVariable UUID id) {
        return programService.getStudentsByProgram(id);
    }
}