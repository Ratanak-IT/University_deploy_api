package com.universitymanagement.classroom.repository;

import com.universitymanagement.classroom.dto.ClassroomRole;
import com.universitymanagement.classroom.dto.MemberStatus;
import com.universitymanagement.classroom.entity.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {
    boolean existsByClassroom_ClassroomIdAndUser_Id(UUID classroomId, UUID userId);
    Optional<ClassroomMember> findByClassroom_ClassroomIdAndUser_Id(UUID classroomId, UUID userId);
    List<ClassroomMember> findByClassroom_ClassroomId(UUID classroomId);
    List<ClassroomMember> findByUser_IdAndRoleAndStatus(
            UUID userId, ClassroomRole role, MemberStatus status);
    List<ClassroomMember> findByClassroom_ClassroomIdAndRoleAndStatus(
            UUID classroomId, ClassroomRole role, MemberStatus status);
    boolean existsByClassroom_ClassroomIdAndUser_IdAndRoleAndStatus(
            UUID classroomId, UUID userId, ClassroomRole role, MemberStatus status);

    /**
     * Members with their user row already loaded.
     *
     * <p>{@code member.user} is lazy, so reading a name, email or avatar off
     * the plain derived query costs one round trip per member.
     */
    @Query("""
            select m from ClassroomMember m
            join fetch m.user u
            where m.classroom.classroomId = :classroomId
              and m.role = :role
              and m.status = :status
            """)
    List<ClassroomMember> findMembersWithUser(@Param("classroomId") UUID classroomId,
                                              @Param("role") ClassroomRole role,
                                              @Param("status") MemberStatus status);
}
