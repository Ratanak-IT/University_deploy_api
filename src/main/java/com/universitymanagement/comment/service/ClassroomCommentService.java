package com.universitymanagement.comment.service;

import com.universitymanagement.comment.dto.request.CommentCreateRequest;
import com.universitymanagement.comment.dto.request.CommentUpdateRequest;
import com.universitymanagement.comment.dto.response.CommentResponse;
import com.universitymanagement.comment.dto.response.MentionUserResponse;

import java.util.List;
import java.util.UUID;


public interface ClassroomCommentService {

    List<CommentResponse> getClassroomComments(UUID classroomId);

    CommentResponse createClassroomComment(UUID classroomId, CommentCreateRequest request);

    List<CommentResponse> getAssignmentComments(UUID assignmentId);

    CommentResponse createAssignmentComment(UUID assignmentId, CommentCreateRequest request);

    CommentResponse updateComment(UUID commentId, CommentUpdateRequest request);

    void deleteComment(UUID commentId);

    List<MentionUserResponse> getMentionableMembers(UUID classroomId, String query);

    List<MentionUserResponse> getMentionableMembersForAssignment(UUID assignmentId, String query);
}
