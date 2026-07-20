package com.universitymanagement.assignment.service;

import com.universitymanagement.assignment.dto.request.AssignmentRequest;
import com.universitymanagement.assignment.dto.request.GradeSubmissionRequest;
import com.universitymanagement.assignment.dto.response.AssignmentResponse;
import com.universitymanagement.assignment.dto.response.SubmissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    AssignmentResponse createAssignment(UUID classroomId, AssignmentRequest request, List<MultipartFile> files);

    List<SubmissionResponse> getSubmissionsByAssignment(UUID assignmentId);

    SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest request);

    List<AssignmentResponse> getAssignmentsByClassroom(UUID classroomId);
    Page<AssignmentResponse> getAllAssignments(int page, int size);

    SubmissionResponse submitAssignment(UUID assignmentId, List<MultipartFile> files);
}
