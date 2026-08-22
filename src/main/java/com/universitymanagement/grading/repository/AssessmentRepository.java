package com.universitymanagement.grading.repository;

import com.universitymanagement.grading.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByComponent_ComponentIdAndIsDeletedFalseOrderByPositionAsc(UUID componentId);

    @Query("""
            select a from Assessment a
            where a.component.classroom.classroomId = :classroomId
              and a.isDeleted = false
            order by a.component.position asc, a.position asc
            """)
    List<Assessment> findByClassroom(UUID classroomId);

    Optional<Assessment> findByComponent_ComponentIdAndAssignment_AssignmentId(
            UUID componentId, UUID assignmentId);

    Optional<Assessment> findByComponent_ComponentIdAndQuiz_QuizId(UUID componentId, UUID quizId);
}
