package com.universitymanagement.subject.repository;

import com.universitymanagement.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Boolean existsBySubjectNameIgnoreCase(String subjectName);
    long countByDepartment_DepartmentId(UUID departmentId);
}
