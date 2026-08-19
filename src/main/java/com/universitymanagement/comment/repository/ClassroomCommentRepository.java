package com.universitymanagement.comment.repository;

import com.universitymanagement.comment.entity.ClassroomComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ClassroomCommentRepository extends JpaRepository<ClassroomComment, UUID> {

    @EntityGraph(attributePaths = {"author", "mentions", "mentions.mentionedUser"})
    @Query("""
            SELECT c FROM ClassroomComment c
            WHERE c.classroom.classroomId = :classroomId
              AND c.assignment IS NULL
              AND c.parent IS NULL
            ORDER BY c.createdAt DESC
            """)
    List<ClassroomComment> findTopLevelForClassroom(UUID classroomId);

    @EntityGraph(attributePaths = {"author", "mentions", "mentions.mentionedUser"})
    @Query("""
            SELECT c FROM ClassroomComment c
            WHERE c.assignment.assignmentId = :assignmentId
              AND c.parent IS NULL
            ORDER BY c.createdAt DESC
            """)
    List<ClassroomComment> findTopLevelForAssignment(UUID assignmentId);

    @EntityGraph(attributePaths = {"author", "mentions", "mentions.mentionedUser"})
    @Query("""
            SELECT c FROM ClassroomComment c
            WHERE c.parent.commentId IN :parentIds
            ORDER BY c.createdAt ASC
            """)
    List<ClassroomComment> findRepliesOf(List<UUID> parentIds);

    long countByClassroom_ClassroomId(UUID classroomId);

    long countByAssignment_AssignmentId(UUID assignmentId);
}