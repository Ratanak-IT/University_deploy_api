package com.universitymanagement.lesson.repository;

import com.universitymanagement.lesson.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByClassroom_ClassroomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID classroomId);
}
