package com.universitymanagement.grading.service;

import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.entity.Submission;
import com.universitymanagement.assignment.entity.SubmissionStatus;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.assignment.repository.SubmissionRepository;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.entity.ScoreStatus;
import com.universitymanagement.grading.repository.AssessmentRepository;
import com.universitymanagement.grading.repository.AssessmentScoreRepository;
import com.universitymanagement.quiz.entity.AttemptStatus;
import com.universitymanagement.quiz.entity.Quiz;
import com.universitymanagement.quiz.entity.QuizAttempt;
import com.universitymanagement.quiz.repository.QuizAttemptRepository;
import com.universitymanagement.quiz.entity.QuizAssignment;
import com.universitymanagement.quiz.repository.QuizAssignmentRepository;
import com.universitymanagement.quiz.repository.QuizRepository;
import com.universitymanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the assignment- and quiz-sourced components in step with the modules
 * that own that work.
 *
 * <p>The old calculator read assignments <em>and</em> exam scores of type
 * ASSIGNMENT into the same total, so one piece of work could count twice under
 * two different weights. Mirroring instead of re-reading gives the gradebook a
 * single row per piece of work, and it is the only writer of those rows.
 */
@Service
@RequiredArgsConstructor
public class LinkedAssessmentSync {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAssignmentRepository quizAssignmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public void sync(UUID classroomId, List<GradeComponent> components) {
        for (GradeComponent component : components) {
            switch (component.getSource()) {
                case ASSIGNMENT -> syncAssignments(classroomId, component);
                case QUIZ -> syncQuizzes(classroomId, component);
                default -> {
                    // MANUAL columns are the teacher's; ATTENDANCE is derived at
                    // calculation time and never materialises assessments.
                }
            }
        }
    }

    private void syncAssignments(UUID classroomId, GradeComponent component) {
        List<Assignment> assignments = assignmentRepository
                .findByClassroom_ClassroomIdAndIsDeletedFalseOrderByDueDateAsc(classroomId);
        if (assignments.isEmpty()) {
            return;
        }

        Map<UUID, Assessment> byAssignment = new HashMap<>();
        int position = 0;
        for (Assignment assignment : assignments) {
            Double max = assignment.getMaxScore();
            if (max == null || max <= 0) {
                continue; // ungraded activity — not part of the course grade
            }

            Assessment assessment = assessmentRepository
                    .findByComponent_ComponentIdAndAssignment_AssignmentId(
                            component.getComponentId(), assignment.getAssignmentId())
                    .orElseGet(Assessment::new);

            assessment.setComponent(component);
            assessment.setAssignment(assignment);
            assessment.setTitle(assignment.getTitle());
            assessment.setMaxScore(max);
            assessment.setDueDate(assignment.getDueDate());
            assessment.setPosition(position++);
            assessment.setIsDeleted(false);

            byAssignment.put(assignment.getAssignmentId(), assessmentRepository.save(assessment));
        }

        if (byAssignment.isEmpty()) {
            return;
        }

        List<Submission> submissions = submissionRepository
                .findByAssignment_AssignmentIdIn(new ArrayList<>(byAssignment.keySet()));

        for (Submission submission : submissions) {
            if (submission.getStatus() != SubmissionStatus.GRADED || submission.getScore() == null) {
                continue;
            }
            Assessment assessment = byAssignment.get(submission.getAssignment().getAssignmentId());
            if (assessment == null) {
                continue;
            }
            upsert(assessment, submission.getStudent().getStudentId(), submission.getScore(),
                    submission.getGradedAt());
        }
    }

    private void syncQuizzes(UUID classroomId, GradeComponent component) {
        List<Quiz> quizzes = quizAssignmentRepository
                .findByClassroom_ClassroomId(classroomId)
                .stream()
                .map(QuizAssignment::getQuiz)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .sorted(java.util.Comparator.comparing(
                        Quiz::getStartAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        if (quizzes.isEmpty()) {
            return;
        }

        Map<UUID, Assessment> byQuiz = new HashMap<>();
        int position = 0;
        for (Quiz quiz : quizzes) {
            Assessment assessment = assessmentRepository
                    .findByComponent_ComponentIdAndQuiz_QuizId(
                            component.getComponentId(), quiz.getQuizId())
                    .orElseGet(Assessment::new);

            assessment.setComponent(component);
            assessment.setQuiz(quiz);
            assessment.setTitle(quiz.getTitle());
            assessment.setDueDate(quiz.getEndAt());
            assessment.setPosition(position++);
            assessment.setIsDeleted(false);
            // maxScore is settled below from the attempts, which is where the
            // quiz's real point total lives.
            if (assessment.getMaxScore() == null) {
                assessment.setMaxScore(100.0);
            }

            byQuiz.put(quiz.getQuizId(), assessmentRepository.save(assessment));
        }

        List<QuizAttempt> attempts = quizAttemptRepository
                .findByQuiz_QuizIdInAndStatus(new ArrayList<>(byQuiz.keySet()), AttemptStatus.SUBMITTED);

        // A student may attempt a quiz several times; the best one is the grade.
        Map<UUID, Map<UUID, QuizAttempt>> best = new HashMap<>();
        Map<UUID, Double> quizMax = new HashMap<>();

        for (QuizAttempt attempt : attempts) {
            if (attempt.getEarnedScore() == null) {
                continue;
            }
            UUID quizId = attempt.getQuiz().getQuizId();
            UUID studentId = attempt.getStudent().getStudentId();

            if (attempt.getTotalScore() != null && attempt.getTotalScore() > 0) {
                quizMax.merge(quizId, attempt.getTotalScore(), Math::max);
            }

            best.computeIfAbsent(quizId, k -> new HashMap<>())
                    .merge(studentId, attempt, (a, b) ->
                            b.getEarnedScore() > a.getEarnedScore() ? b : a);
        }

        for (Map.Entry<UUID, Assessment> entry : byQuiz.entrySet()) {
            Assessment assessment = entry.getValue();
            Double max = quizMax.get(entry.getKey());
            if (max != null && max > 0 && !max.equals(assessment.getMaxScore())) {
                assessment.setMaxScore(max);
                assessmentRepository.save(assessment);
            }

            for (Map.Entry<UUID, QuizAttempt> scored :
                    best.getOrDefault(entry.getKey(), Map.of()).entrySet()) {
                upsert(assessment, scored.getKey(), scored.getValue().getEarnedScore(),
                        scored.getValue().getSubmittedAt());
            }
        }
    }

    private void upsert(Assessment assessment, UUID studentId, Double score, LocalDateTime gradedAt) {
        AssessmentScore existing = scoreRepository
                .findByAssessment_AssessmentIdAndStudent_StudentId(
                        assessment.getAssessmentId(), studentId)
                .orElse(null);

        if (existing != null
                && existing.getScore() != null
                && existing.getScore().equals(score)) {
            return; // already in step — skip the write
        }

        AssessmentScore target = existing != null ? existing : new AssessmentScore();
        if (existing == null) {
            target.setAssessment(assessment);
            // A lazy reference is enough: the row only needs the foreign key, and
            // loading each student in full would defeat the point of a bulk sync.
            target.setStudent(studentRepository.getReferenceById(studentId));
        }
        target.setScore(score);
        target.setStatus(ScoreStatus.GRADED);
        target.setGradedAt(gradedAt != null ? gradedAt : LocalDateTime.now());

        scoreRepository.save(target);
    }
}
