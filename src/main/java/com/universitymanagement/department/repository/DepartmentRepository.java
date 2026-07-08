package com.universitymanagement.department.repository;

import com.universitymanagement.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Boolean existsByDepartmentNameIgnoreCase(String departmentName);
}
