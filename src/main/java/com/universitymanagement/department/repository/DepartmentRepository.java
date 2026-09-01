package com.universitymanagement.department.repository;

import com.universitymanagement.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Boolean existsByDepartmentNameIgnoreCase(String departmentName);
    boolean existsByDepartmentCode(String departmentCode);
    Optional<Department> findFirstByOrderByDepartmentIdDesc();
    Page<Department> findByIsDeleted(Boolean isDeleted, Pageable pageable);   // ← បន្ថែម

    /* ---- counts, gathered for a whole page in one query ---- */

    @Query("""
            select new com.universitymanagement.department.repository.DepartmentCount(
                       d.departmentId, count(t))
            from Teacher t join t.departments d
            group by d.departmentId
            """)
    List<DepartmentCount> countTeachersPerDepartment();

    @Query("""
            select new com.universitymanagement.department.repository.DepartmentCount(
                       s.department.departmentId, count(s))
            from Subject s
            where s.department is not null
              and (s.isDeleted is null or s.isDeleted = false)
            group by s.department.departmentId
            """)
    List<DepartmentCount> countSubjectsPerDepartment();

    @Query("select count(t) from Teacher t join t.departments d where d.departmentId = :id")
    long countTeachersIn(@Param("id") UUID departmentId);

    // Soft-deleted subjects are excluded, so the detail view agrees with the
    // subject list rather than counting rows the user cannot see.
    @Query("""
            select count(s) from Subject s
            where s.department.departmentId = :id
              and (s.isDeleted is null or s.isDeleted = false)
            """)
    long countSubjectsIn(@Param("id") UUID departmentId);
}
