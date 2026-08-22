package com.universitymanagement.score.service.impl;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.entity.ScoreStatus;
import com.universitymanagement.grading.repository.AssessmentRepository;
import com.universitymanagement.grading.repository.AssessmentScoreRepository;
import com.universitymanagement.grading.security.ClassroomGradeGuard;
import com.universitymanagement.grading.service.GradeSchemeService;
import com.universitymanagement.score.dto.request.SetExamScoresRequest;
import com.universitymanagement.score.dto.response.ExamScoreResponse;
import com.universitymanagement.score.entity.ExamType;
import com.universitymanagement.score.exception.InvalidExamScoreException;
import com.universitymanagement.score.exception.StudentNotInClassroomException;
import com.universitymanagement.score.service.ExamScoreService;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.teacher.entity.Teacher;
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
 * Compatibility layer over the old flat exam-score API.
 *
 * <p>It now reads and writes the gradebook rather than its own table, so the
 * two can no longer disagree. Exam types that the gradebook derives from
 * another module — assignments, quizzes, attendance — are refused here instead
 * of being written a second time under a different weight.
 *
 * @deprecated use the classroom gradebook endpoints; this exists so older
 * clients keep working through the transition.
 */
@Deprecated
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamScoreServiceImpl implements ExamScoreService {

    /** Exam types that map onto a manual component of the default scheme. */
    private static final Map<ExamType, String> MANUAL_COMPONENTS = Map.of(
            ExamType.MIDTERM, "Midterm",
            ExamType.FINAL, "Final Exam");

    private static final Map<ExamType, String> DERIVED_ELSEWHERE = Map.of(
            ExamType.ASSIGNMENT, "the assignments module",
            ExamType.QUIZ, "the quizzes module",
            ExamType.ATTENDANCE, "the attendance register");

    private final ClassroomGradeGuard guard;
    private final GradeSchemeService schemeService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final ClassroomStudentRepository classroomStudentRepository;

    @Override
    @Transactional
    public List<ExamScoreResponse> setScores(UUID classroomId, SetExamScoresRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        Teacher teacher = guard.requireGrader(classroom);

        String derivedFrom = DERIVED_ELSEWHERE.get(request.examType());
        if (derivedFrom != null) {
            throw new InvalidExamScoreException(
                    request.examType() + " scores are derived from " + derivedFrom
                            + " and can no longer be typed in here.");
        }

        String componentName = MANUAL_COMPONENTS.get(request.examType());
        if (componentName == null) {
            throw new InvalidExamScoreException(
                    "Exam type " + request.examType() + " has no fixed place in the grading policy. "
                            + "Add a column to the classroom gradebook instead.");
        }

        Assessment assessment = resolveAssessment(classroom, componentName, request.maxScore());

        Map<UUID, Student> roster = new HashMap<>();
        for (ClassroomStudent link : classroomStudentRepository
                .findByClassroom_ClassroomId(classroomId)) {
            if (link.getStudent() != null) {
                roster.put(link.getStudent().getStudentId(), link.getStudent());
            }
        }

        List<ExamScoreResponse> saved = new ArrayList<>();
        for (SetExamScoresRequest.StudentScore item : request.scores()) {
            if (item.score() > request.maxScore()) {
                throw new InvalidExamScoreException(
                        "Score " + item.score() + " exceeds maxScore " + request.maxScore()
                                + " for student " + item.studentId());
            }

            Student student = roster.get(item.studentId());
            if (student == null) {
                throw new StudentNotInClassroomException(item.studentId(), classroomId);
            }

            AssessmentScore score = scoreRepository
                    .findByAssessment_AssessmentIdAndStudent_StudentId(
                            assessment.getAssessmentId(), student.getStudentId())
                    .orElseGet(AssessmentScore::new);

            score.setAssessment(assessment);
            score.setStudent(student);
            score.setScore(item.score());
            score.setStatus(ScoreStatus.GRADED);
            score.setGradedByTeacher(teacher);
            score.setGradedAt(LocalDateTime.now());

            saved.add(toResponse(classroomId, request.examType(),
                    scoreRepository.save(score), assessment));
        }
        return saved;
    }

    @Override
    public List<ExamScoreResponse> getScoresByClassroom(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);

        List<ExamScoreResponse> responses = new ArrayList<>();
        for (AssessmentScore score : scoreRepository.findByClassroom(classroomId)) {
            Assessment assessment = score.getAssessment();
            responses.add(toResponse(classroomId,
                    examTypeOf(assessment.getComponent()), score, assessment));
        }
        return responses;
    }

    // ---- helpers ----

    private Assessment resolveAssessment(Classroom classroom, String componentName, Double maxScore) {
        GradeComponent component = schemeService.ensureScheme(classroom).stream()
                .filter(c -> c.getSource() == ComponentSource.MANUAL)
                .filter(c -> c.getName().equalsIgnoreCase(componentName))
                .findFirst()
                .orElseThrow(() -> new InvalidExamScoreException(
                        "This classroom's grading policy has no manual \"" + componentName
                                + "\" component. Edit the policy in the gradebook first."));

        List<Assessment> assessments = assessmentRepository
                .findByComponent_ComponentIdAndIsDeletedFalseOrderByPositionAsc(
                        component.getComponentId());

        // The old API had one score per exam type, so it maps onto exactly one
        // column. If the teacher has since split the component into several,
        // there is no unambiguous target and they should use the gradebook.
        if (assessments.size() > 1) {
            throw new InvalidExamScoreException(
                    "\"" + component.getName() + "\" now has " + assessments.size()
                            + " columns. Enter these scores in the gradebook instead.");
        }

        Assessment assessment = assessments.isEmpty() ? new Assessment() : assessments.getFirst();
        if (assessments.isEmpty()) {
            assessment.setComponent(component);
            assessment.setTitle(component.getName());
            assessment.setPosition(0);
        }
        assessment.setMaxScore(maxScore);
        return assessmentRepository.save(assessment);
    }

    private ExamType examTypeOf(GradeComponent component) {
        return switch (component.getSource()) {
            case ASSIGNMENT -> ExamType.ASSIGNMENT;
            case QUIZ -> ExamType.QUIZ;
            case ATTENDANCE -> ExamType.ATTENDANCE;
            case MANUAL -> MANUAL_COMPONENTS.entrySet().stream()
                    .filter(e -> e.getValue().equalsIgnoreCase(component.getName()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(ExamType.OTHER);
        };
    }

    private ExamScoreResponse toResponse(UUID classroomId, ExamType examType,
                                         AssessmentScore score, Assessment assessment) {
        Student student = score.getStudent();
        return new ExamScoreResponse(
                score.getScoreId(),
                student.getStudentId(),
                student.getStudentCode(),
                student.getUser() != null ? student.getUser().getFullName() : null,
                classroomId,
                examType,
                score.getScore(),
                assessment.getMaxScore());
    }
}
