package com.universitymanagement.assignment.repository;

import com.universitymanagement.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByAssignment_AssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);
    Optional<Submission> findByAssignment_AssignmentIdAndStudent_StudentId(UUID assignmentId, UUID studentId);
}
