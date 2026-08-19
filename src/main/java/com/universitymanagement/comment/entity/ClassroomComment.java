package com.universitymanagement.comment.entity;

import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "classroom_comments", indexes = {
        @Index(name = "idx_comment_classroom", columnList = "classroom_id"),
        @Index(name = "idx_comment_assignment", columnList = "assignment_id"),
        @Index(name = "idx_comment_parent", columnList = "parent_comment_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ClassroomComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id")
    private UUID commentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private ClassroomComment parent;


    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassroomComment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentMention> mentions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isEdited() {
        return updatedAt != null && updatedAt.isAfter(createdAt);
    }
}