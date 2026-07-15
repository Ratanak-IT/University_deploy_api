package com.universitymanagement.department.service;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.teacher.dto.response.TeacherResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentRequest departmentRequest);
    Page<DepartmentResponse> getAllDepartments(int page, int size);
    DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest departmentRequest);
    void delete(UUID departmentId);
    void softDelete(UUID departmentId);
    DepartmentResponse getDepartmentById(UUID departmentId);
    Page<DepartmentResponse> getByStatus(int page, int size, Boolean isDeleted);
    List<TeacherResponse> getTeachersByDepartment(UUID departmentId);
}
