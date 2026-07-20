package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.entity.Classroom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {
    List<Classroom> findByTeacher_TeacherIdAndIsDeletedFalse(UUID teacherId);
    List<Classroom> findByTeacher_TeacherId(UUID teacherId);
    Page<Classroom> findByIsDeletedFalse(Pageable pageable);
    boolean existsByInviteCode(String code);
    long countBySubject_SubjectId(UUID subjectId);
    boolean existsByClassCode(String classCode);

    @Query("""
        select c from Classroom c
        where c.isDeleted = false
          and (:keyword is null
               or lower(c.className) like lower(concat('%', cast(:keyword as string), '%'))
               or lower(c.classCode) like lower(concat('%', cast(:keyword as string), '%')))
          and (:programId is null or c.program.id = :programId)
          and (:yearLevel is null or c.yearLevel = :yearLevel)
          and (:semester is null or c.semester = :semester)
        """)
    Page<Classroom> search(
            @Param("keyword") String keyword,
            @Param("programId") UUID programId,
            @Param("yearLevel") Integer yearLevel,
            @Param("semester") Integer semester,
            Pageable pageable);
}
