package com.universitymanagement.student.repository;

import com.universitymanagement.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByUserId(UUID id);
    boolean existsByStudentCode(String studentCode);

    @Query("""
            select s from Student s
            join s.user u
            where lower(s.studentCode) like lower(concat('%', :keyword, '%'))
               or lower(u.fullName)    like lower(concat('%', :keyword, '%'))
               or lower(u.email)       like lower(concat('%', :keyword, '%'))
            """)
    Page<Student> search(@Param("keyword") String keyword, Pageable pageable);
    List<Student> findByProgram_Id(UUID programId);
}
