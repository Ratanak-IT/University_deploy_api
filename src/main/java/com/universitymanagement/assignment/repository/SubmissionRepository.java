package com.universitymanagement.assignment.repository;

import com.universitymanagement.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByAssignment_AssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);
    Optional<Submission> findByAssignment_AssignmentIdAndStudent_StudentId(UUID assignmentId, UUID studentId);

    /**
     * Same as {@link #findByAssignment_AssignmentIdOrderBySubmittedAtDesc},
     * with the student, their user row, and the submission's own files all
     * already loaded. The teacher's submissions list reads every one of
     * those per row (name, avatar, attachments) — against a remote database
     * that was three extra round trips per submission for nothing.
     */
    @Query("""
            select distinct s from Submission s
            left join fetch s.student st
            left join fetch st.user
            left join fetch s.files
            where s.assignment.assignmentId = :assignmentId
            order by s.submittedAt desc
            """)
    List<Submission> findByAssignmentWithDetails(@Param("assignmentId") UUID assignmentId);

    /** All submissions for a set of assignments — lets the gradebook mirror them in one query. */
    List<Submission> findByAssignment_AssignmentIdIn(List<UUID> assignmentIds);

    /**
     * One student's submissions across a set of assignments, files included.
     * Replaces looking a submission up per assignment (plus a further lazy
     * load per submission just for its files) with a single round trip.
     */
    @Query("""
            select distinct s from Submission s
            left join fetch s.files
            where s.assignment.assignmentId in :assignmentIds
            and s.student.studentId = :studentId
            """)
    List<Submission> findByAssignmentIdsAndStudentWithFiles(
            @Param("assignmentIds") List<UUID> assignmentIds,
            @Param("studentId") UUID studentId);

    /**
     * Same lookup, without touching files — for list views that only need
     * status and score. Avoids the per-file MinIO URL signing that reading
     * {@code submission.getFiles()} would trigger.
     */
    List<Submission> findByAssignment_AssignmentIdInAndStudent_StudentId(
            List<UUID> assignmentIds, UUID studentId);

    /** Ungraded submissions across a set of classrooms, for the teacher dashboard's "to grade" count. */
    @Query("""
            select count(s) from Submission s
            where s.assignment.classroom.classroomId in :classroomIds
              and s.score is null
            """)
    long countUngradedByClassroomIds(@Param("classroomIds") List<UUID> classroomIds);

    /** How many submissions each assignment has. */
    interface AssignmentSubmissionCount {
        UUID getAssignmentId();

        long getTotal();
    }

    /**
     * Submission counts for a set of assignments in one query, rather than
     * one count per assignment card on the classroom's assignments tab.
     */
    @Query("""
            select s.assignment.assignmentId as assignmentId, count(s) as total
            from Submission s
            where s.assignment.assignmentId in :assignmentIds
            group by s.assignment.assignmentId
            """)
    List<AssignmentSubmissionCount> countByAssignmentIds(@Param("assignmentIds") List<UUID> assignmentIds);
}
