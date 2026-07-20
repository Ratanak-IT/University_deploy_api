package com.universitymanagement.department.controller;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.service.DepartmentService;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DepartmentResponse createDepartment( @Valid @RequestBody DepartmentRequest departmentRequest) {
        return departmentService.createDepartment(departmentRequest);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public Page<DepartmentResponse> getAllDepartments(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "25") int size){
        return departmentService.getAllDepartments(page, size);
    }

    @PutMapping("/{departmentId}")
    public DepartmentResponse updateDepartment(@PathVariable UUID departmentId, @Valid @RequestBody DepartmentRequest departmentRequest) {
        return departmentService.updateDepartment(departmentId, departmentRequest);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping( "/{departmentId}")
    public void delete(@PathVariable UUID departmentId) {
        departmentService.delete(departmentId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{departmentId}")
    public void softDelete(@PathVariable UUID departmentId) {
        departmentService.softDelete(departmentId);
    }

    @GetMapping("/{departmentId}")
    public DepartmentResponse getDepartmentById(
            @PathVariable UUID departmentId
    ) {
        return departmentService.getDepartmentById(departmentId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/status")
    public Page<DepartmentResponse> getByStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) Boolean isDeleted) {
        return departmentService.getByStatus(page, size, isDeleted);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{departmentId}/teachers")
    public List<TeacherResponse> getTeachersByDepartment(@PathVariable UUID departmentId) {
        return departmentService.getTeachersByDepartment(departmentId);
    }

}
