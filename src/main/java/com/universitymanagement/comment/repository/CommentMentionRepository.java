package com.universitymanagement.comment.repository;

import com.universitymanagement.comment.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentMentionRepository extends JpaRepository<CommentMention, UUID> {
    void deleteByComment_CommentId(UUID commentId);
}
