package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.entity.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {
    boolean existsByClassroom_ClassroomIdAndUser_Id(UUID classroomId, UUID userId);
    Optional<ClassroomMember> findByClassroom_ClassroomIdAndUser_Id(UUID classroomId, UUID userId);
    List<ClassroomMember> findByClassroom_ClassroomId(UUID classroomId);

}
