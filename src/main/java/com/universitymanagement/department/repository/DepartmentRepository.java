package com.universitymanagement.department.repository;

import com.universitymanagement.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Boolean existsByDepartmentNameIgnoreCase(String departmentName);
    boolean existsByDepartmentCode(String departmentCode);
    Optional<Department> findFirstByOrderByDepartmentIdDesc();
    Page<Department> findByIsDeleted(Boolean isDeleted, Pageable pageable);   // ← បន្ថែម

}
