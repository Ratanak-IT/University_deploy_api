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
}