package com.universitymanagement.student.controller;

import com.universitymanagement.student.dto.request.CreateStudentRequest;
import com.universitymanagement.student.dto.request.StudentUpdateRequest;
import com.universitymanagement.student.dto.response.StudentAdminResponse;
import com.universitymanagement.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StudentAdminController {

    private final StudentService studentService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public Page<StudentAdminResponse> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String keyword) {
        return studentService.getAllStudents(page, size, keyword);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{studentId}")
    public StudentAdminResponse getStudentById(@PathVariable UUID studentId) {
        return studentService.getStudentById(studentId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public StudentAdminResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.createStudent(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{studentId}")
    public StudentAdminResponse updateStudent(@PathVariable UUID studentId,
                                              @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.updateStudent(studentId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{studentId}")
    public void deleteStudent(@PathVariable UUID studentId) {
        studentService.deleteStudent(studentId);
    }
}
