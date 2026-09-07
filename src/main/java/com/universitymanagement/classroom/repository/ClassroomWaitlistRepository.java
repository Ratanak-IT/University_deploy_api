package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.entity.ClassroomWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomWaitlistRepository extends JpaRepository<ClassroomWaitlist, Long> {
    boolean existsByClassroom_ClassroomIdAndStudent_StudentId(UUID classroomId, UUID studentId);

    List<ClassroomWaitlist> findByClassroom_ClassroomIdOrderByRequestedAtAsc(UUID classroomId);

    Optional<ClassroomWaitlist> findFirstByClassroom_ClassroomIdOrderByRequestedAtAsc(UUID classroomId);
}
