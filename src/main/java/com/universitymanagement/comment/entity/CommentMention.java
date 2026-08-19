package com.universitymanagement.comment.entity;

import com.universitymanagement.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "comment_mentions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_comment_mentioned_user",
                columnNames = {"comment_id", "mentioned_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class CommentMention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mention_id")
    private UUID mentionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private ClassroomComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentioned_user_id", nullable = false)
    private User mentionedUser;

    public CommentMention(ClassroomComment comment, User mentionedUser) {
        this.comment = comment;
        this.mentionedUser = mentionedUser;
    }
}
