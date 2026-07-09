package com.universitymanagement.assignment.repository;

import com.universitymanagement.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc(UUID classroomId);
}
