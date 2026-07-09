package com.universitymanagement.student.repository;

import com.universitymanagement.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByUserId(UUID id);
    boolean existsByStudentCode(String studentCode);

}
