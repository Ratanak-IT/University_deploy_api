package com.universitymanagement.assignment.entity;

import com.universitymanagement.identity.entity.User;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One message in the private thread between a student and their teacher(s)
 * about one assignment — the "Private comments" panel that previously posted
 * nowhere.
 *
 * <p>Unlike {@code ClassroomComment}, this is not visible to classmates: a
 * thread is scoped to exactly one (assignment, student) pair, and only that
 * student plus the teacher(s) of the assignment's classroom can read it. It
 * is deliberately flat — no replies-to-replies, no mentions — matching what
 * the feature actually needs: a running back-and-forth about one student's
 * work, not a public discussion.
 */
@Entity
@Table(name = "private_comments", indexes = {
        @Index(name = "idx_private_comment_thread", columnList = "assignment_id, student_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PrivateComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id")
    private UUID commentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /** Whose thread this is — not necessarily who wrote this message. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
