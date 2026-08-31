package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.entity.ClassroomStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudent, Long> {
    List<ClassroomStudent> findByStudent_StudentId(UUID studentId);
    boolean existsByClassroom_ClassroomIdAndStudent_StudentId(UUID classroomId, UUID studentId);
    Optional<ClassroomStudent> findByClassroom_ClassroomIdAndStudent_StudentId(UUID classroomId, UUID studentId);
    List<ClassroomStudent> findByClassroom_ClassroomId(UUID classroomId);
    long countByClassroom_ClassroomId(UUID classroomId);
    List<ClassroomStudent> findByClassroom_ClassroomIdIn(List<UUID> classroomIds);

    /**
     * The roster with each student's user row already loaded.
     *
     * <p>{@code student.user} is lazy, so reading a name, email or avatar off
     * the plain derived query costs one extra round trip per student. Against a
     * remote database that is the difference between one query and thirty.
     */
    @Query("""
            select cs from ClassroomStudent cs
            join fetch cs.student s
            left join fetch s.user u
            where cs.classroom.classroomId = :classroomId
            """)
    List<ClassroomStudent> findRosterWithUser(@Param("classroomId") UUID classroomId);

    /**
     * A student's own enrolments, with each classroom and its subject already
     * loaded. Both are lazy, so reading a class name or subject off the plain
     * derived query costs two extra round trips per classroom — for a student
     * in six classes that is twelve avoidable queries every time their
     * schedule, grades or assignments are read.
     */
    @Query("""
            select cs from ClassroomStudent cs
            join fetch cs.classroom c
            left join fetch c.subject
            where cs.student.studentId = :studentId
            """)
    List<ClassroomStudent> findByStudentWithClassroomAndSubject(@Param("studentId") UUID studentId);

    /**
     * How many distinct students are enrolled across a set of classrooms —
     * a student in more than one of the teacher's classrooms is only
     * counted once. Feeds the teacher dashboard summary in one query
     * instead of loading every roster row.
     */
    @Query("""
            select count(distinct cs.student.studentId) from ClassroomStudent cs
            where cs.classroom.classroomId in :classroomIds
            """)
    long countDistinctStudentsByClassroomIds(@Param("classroomIds") List<UUID> classroomIds);
}