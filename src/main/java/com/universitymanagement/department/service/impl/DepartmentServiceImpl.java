package com.universitymanagement.department.service.impl;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.department.mapper.Departmentmapper;
import com.universitymanagement.department.repository.DepartmentRepository;
import com.universitymanagement.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final Departmentmapper departmentmapper;
    @Override
    public DepartmentResponse createDepartment(DepartmentRequest departmentRequest) {
        if (departmentRepository.existsByDepartmentNameIgnoreCase(departmentRequest.departmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }
        Department department = departmentmapper.toEntity(departmentRequest);
        Department savedDepartment = departmentRepository.save(department);
        return departmentmapper.toResponse(savedDepartment);
    }

    @Override
    public Page<DepartmentResponse> getAllDepartments(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "departmentName");
        Pageable pageable = PageRequest.of(page, size,sort);
       return departmentRepository.findAll(pageable).map(departmentmapper::toResponse);
    }

    @Override
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest departmentRequest) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        String departmentName = departmentRequest.departmentName().trim();
        if (!department.getDepartmentName().equalsIgnoreCase(departmentName)
        && departmentRepository.existsByDepartmentNameIgnoreCase(departmentName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department '" + departmentName + "' already exists.");
        }
        department.setDepartmentName(departmentName);
        Department updatedDepartment = departmentRepository.save(department);
        return departmentmapper.toResponse(updatedDepartment);
    }

    @Override
    public void delete(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        departmentRepository.delete(department);
    }

    @Override
    public void softDelete(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        department.setIsDeleted(true);
    }

    @Override
    public DepartmentResponse getDepartmentById(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        return departmentmapper.toResponse(department);
    }
}
