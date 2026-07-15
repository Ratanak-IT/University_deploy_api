package com.universitymanagement.subject.repository;

import com.universitymanagement.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Boolean existsBySubjectNameIgnoreCase(String subjectName);
    long countByDepartment_DepartmentId(UUID departmentId);
    Optional<Subject> findFirstByOrderBySubjectCodeDesc();
    @Query(value = "SELECT s.subject_code FROM subjects s " +
            "WHERE s.subject_code LIKE 'SUB-%' " +
            "ORDER BY CAST(SUBSTRING(s.subject_code FROM 5) AS INTEGER) DESC " +
            "LIMIT 1", nativeQuery = true)
    Optional<String> findLastSubjectCode();

}
