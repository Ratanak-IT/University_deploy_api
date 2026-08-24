package com.universitymanagement.assignment.service;

import com.universitymanagement.assignment.dto.request.CreatePrivateCommentRequest;
import com.universitymanagement.assignment.dto.response.PrivateCommentResponse;

import java.util.List;
import java.util.UUID;

public interface PrivateCommentService {

    /** A student reading their own thread for one assignment. */
    List<PrivateCommentResponse> getMyThread(UUID assignmentId);

    /** A student posting to their own thread. */
    PrivateCommentResponse postToMyThread(UUID assignmentId, CreatePrivateCommentRequest request);

    /** A teacher (or admin) reading one specific student's thread. */
    List<PrivateCommentResponse> getStudentThread(UUID assignmentId, UUID studentId);

    /** A teacher (or admin) posting to one specific student's thread. */
    PrivateCommentResponse postToStudentThread(UUID assignmentId, UUID studentId,
                                               CreatePrivateCommentRequest request);
}
