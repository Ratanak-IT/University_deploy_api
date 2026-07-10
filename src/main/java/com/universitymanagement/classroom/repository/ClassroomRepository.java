package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.entity.Classroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {
    List<Classroom> findByTeacher_TeacherIdAndIsDeletedFalse(UUID teacherId);
    List<Classroom> findByTeacher_TeacherId(UUID teacherId);
    Page<Classroom> findByIsDeletedFalse(Pageable pageable);
    boolean existsByClassCode(String code);
    boolean existsByInviteCode(String code);
}
