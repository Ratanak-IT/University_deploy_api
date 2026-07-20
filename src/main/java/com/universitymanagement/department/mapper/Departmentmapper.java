package com.universitymanagement.department.mapper;

import com.universitymanagement.department.dto.request.DepartmentRequest;
import com.universitymanagement.department.dto.response.DepartmentResponse;
import com.universitymanagement.department.entity.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface Departmentmapper {
    DepartmentResponse toResponse(Department department);
    Department toEntity(DepartmentRequest departmentRequest);
}
