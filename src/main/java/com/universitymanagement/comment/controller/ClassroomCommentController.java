package com.universitymanagement.comment.controller;

import com.universitymanagement.comment.dto.request.CommentCreateRequest;
import com.universitymanagement.comment.dto.request.CommentUpdateRequest;
import com.universitymanagement.comment.dto.response.CommentResponse;
import com.universitymanagement.comment.dto.response.MentionUserResponse;
import com.universitymanagement.comment.service.ClassroomCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomCommentController {

    private static final String ANY_MEMBER = "hasAnyRole('ADMIN','TEACHER','STUDENT')";

    private final ClassroomCommentService commentService;


    @GetMapping("/classrooms/{classroomId}/comments")
    @PreAuthorize(ANY_MEMBER)
    public List<CommentResponse> getClassroomComments(@PathVariable UUID classroomId) {
        return commentService.getClassroomComments(classroomId);
    }

    @PostMapping("/classrooms/{classroomId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ANY_MEMBER)
    public CommentResponse createClassroomComment(
            @PathVariable UUID classroomId,
            @Valid @RequestBody CommentCreateRequest request) {
        return commentService.createClassroomComment(classroomId, request);
    }

    @GetMapping("/classrooms/{classroomId}/mentionable-members")
    @PreAuthorize(ANY_MEMBER)
    public List<MentionUserResponse> getClassroomMentionable(
            @PathVariable UUID classroomId,
            @RequestParam(required = false) String query) {
        return commentService.getMentionableMembers(classroomId, query);
    }


    @GetMapping("/assignments/{assignmentId}/comments")
    @PreAuthorize(ANY_MEMBER)
    public List<CommentResponse> getAssignmentComments(@PathVariable UUID assignmentId) {
        return commentService.getAssignmentComments(assignmentId);
    }

    @PostMapping("/assignments/{assignmentId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ANY_MEMBER)
    public CommentResponse createAssignmentComment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CommentCreateRequest request) {
        return commentService.createAssignmentComment(assignmentId, request);
    }

    /** Same roster as the classroom's, reached through the assignment. */
    @GetMapping("/assignments/{assignmentId}/mentionable-members")
    @PreAuthorize(ANY_MEMBER)
    public List<MentionUserResponse> getAssignmentMentionable(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String query) {
        return commentService.getMentionableMembersForAssignment(assignmentId, query);
    }


    @PatchMapping("/comments/{commentId}")
    @PreAuthorize(ANY_MEMBER)
    public CommentResponse updateComment(@PathVariable UUID commentId,
                                         @Valid @RequestBody CommentUpdateRequest request) {
        return commentService.updateComment(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ANY_MEMBER)
    public void deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
    }
}
