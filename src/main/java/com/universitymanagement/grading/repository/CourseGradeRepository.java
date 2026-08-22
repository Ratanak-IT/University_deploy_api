package com.universitymanagement.grading.repository;

import com.universitymanagement.grading.entity.CourseGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseGradeRepository extends JpaRepository<CourseGrade, UUID> {

    Optional<CourseGrade> findByStudent_StudentIdAndClassroom_ClassroomId(
            UUID studentId, UUID classroomId);

    List<CourseGrade> findByClassroom_ClassroomId(UUID classroomId);

    List<CourseGrade> findByClassroom_ClassroomIdIn(List<UUID> classroomIds);

    /**
     * A student's whole academic history, with the classroom and subject joined
     * so building a transcript costs one query instead of one per course.
     */
    @Query("""
            select g from CourseGrade g
            join fetch g.classroom c
            left join fetch c.subject
            where g.student.studentId = :studentId
            """)
    List<CourseGrade> findByStudentWithCourse(UUID studentId);

    @Query("""
            select g from CourseGrade g
            join fetch g.classroom c
            left join fetch c.subject
            join fetch g.student s
            where c.classroomId in :classroomIds
            """)
    List<CourseGrade> findByClassroomsWithCourse(List<UUID> classroomIds);

    boolean existsByStudent_StudentIdAndClassroom_ClassroomId(UUID studentId, UUID classroomId);

    /** Every grade for a whole cohort in one query, for the transcript list. */
    @Query("""
            select g from CourseGrade g
            join fetch g.classroom c
            left join fetch c.subject
            where g.student.studentId in :studentIds
            """)
    List<CourseGrade> findByStudentIds(List<UUID> studentIds);
}
