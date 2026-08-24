package com.universitymanagement.assignment.repository;

import com.universitymanagement.assignment.entity.PrivateComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PrivateCommentRepository extends JpaRepository<PrivateComment, UUID> {

    /**
     * A thread with each message's author already loaded — author.user is
     * lazy, and every row needs a name and avatar to render.
     */
    @Query("""
            select pc from PrivateComment pc
            join fetch pc.author a
            where pc.assignment.assignmentId = :assignmentId
              and pc.student.studentId = :studentId
            order by pc.createdAt asc
            """)
    List<PrivateComment> findThread(@Param("assignmentId") UUID assignmentId,
                                    @Param("studentId") UUID studentId);
}
