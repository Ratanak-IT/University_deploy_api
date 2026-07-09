package com.universitymanagement.assignment.service;

import com.universitymanagement.assignment.dto.request.AssignmentRequest;
import com.universitymanagement.assignment.dto.request.GradeSubmissionRequest;
import com.universitymanagement.assignment.dto.response.AssignmentResponse;
import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    AssignmentResponse createAssignment(UUID classroomId, AssignmentRequest request, MultipartFile file);

    List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId);

    SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest request);

    List<AssignmentResponse> getAssignmentsByClassroom(UUID classroomId);

    SubmissionResponse submitAssignment(UUID assignmentId, MultipartFile file);
}
