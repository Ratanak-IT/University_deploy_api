package com.universitymanagement.lesson.repository;

import com.universitymanagement.lesson.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByClassroom_ClassroomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID classroomId);
    List<Lesson> findByClassroomIsNullAndCreatedByAndIsDeletedFalseOrderByCreatedAtDesc(String createdBy);

    /** Lesson count across a set of classrooms, for the teacher dashboard's "course materials" stat. */
    @Query("""
            select count(l) from Lesson l
            where l.classroom.classroomId in :classroomIds
              and l.isDeleted = false
            """)
    long countByClassroomIds(@Param("classroomIds") List<UUID> classroomIds);
}
