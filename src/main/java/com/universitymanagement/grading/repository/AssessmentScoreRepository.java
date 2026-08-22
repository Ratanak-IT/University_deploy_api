package com.universitymanagement.grading.repository;

import com.universitymanagement.grading.entity.AssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, UUID> {

    Optional<AssessmentScore> findByAssessment_AssessmentIdAndStudent_StudentId(
            UUID assessmentId, UUID studentId);

    List<AssessmentScore> findByAssessment_AssessmentIdIn(List<UUID> assessmentIds);

    /** Every score in a classroom in one query — the gradebook's whole grid. */
    @Query("""
            select s from AssessmentScore s
            join fetch s.assessment a
            where a.component.classroom.classroomId = :classroomId
              and a.isDeleted = false
            """)
    List<AssessmentScore> findByClassroom(UUID classroomId);

    @Query("""
            select s from AssessmentScore s
            join fetch s.assessment a
            where a.component.classroom.classroomId in :classroomIds
              and s.student.studentId = :studentId
              and a.isDeleted = false
            """)
    List<AssessmentScore> findByClassroomsAndStudent(List<UUID> classroomIds, UUID studentId);

    @Query("""
            select s from AssessmentScore s
            join fetch s.assessment a
            join fetch s.student
            where a.component.classroom.classroomId in :classroomIds
              and a.isDeleted = false
            """)
    List<AssessmentScore> findByClassrooms(List<UUID> classroomIds);
}
