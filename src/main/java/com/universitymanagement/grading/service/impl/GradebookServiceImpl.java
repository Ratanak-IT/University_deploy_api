package com.universitymanagement.grading.service.impl;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.grading.calc.ComponentResult;
import com.universitymanagement.grading.calc.CourseResult;
import com.universitymanagement.grading.calc.GradeCalculator;
import com.universitymanagement.grading.calc.GradingContext;
import com.universitymanagement.grading.dto.request.AssessmentRequest;
import com.universitymanagement.grading.dto.request.SaveScoresRequest;
import com.universitymanagement.grading.dto.response.AssessmentResponse;
import com.universitymanagement.grading.dto.response.GradeComponentResponse;
import com.universitymanagement.grading.dto.response.GradebookResponse;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.CourseGrade;
import com.universitymanagement.grading.entity.CourseGradeStatus;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.entity.LetterGrade;
import com.universitymanagement.grading.entity.ScoreStatus;
import com.universitymanagement.grading.exception.AssessmentNotFoundException;
import com.universitymanagement.grading.exception.GradeComponentNotFoundException;
import com.universitymanagement.grading.exception.GradeLockedException;
import com.universitymanagement.grading.exception.InvalidGradingSchemeException;
import com.universitymanagement.grading.repository.AssessmentRepository;
import com.universitymanagement.grading.repository.AssessmentScoreRepository;
import com.universitymanagement.grading.repository.CourseGradeRepository;
import com.universitymanagement.grading.repository.GradeComponentRepository;
import com.universitymanagement.grading.security.ClassroomGradeGuard;
import com.universitymanagement.grading.service.GradeSchemeService;
import com.universitymanagement.grading.service.GradebookService;
import com.universitymanagement.grading.service.GradingContextLoader;
import com.universitymanagement.grading.service.LinkedAssessmentSync;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.teacher.entity.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradebookServiceImpl implements GradebookService {

    private static final double SCHEME_TOLERANCE = 0.01;

    private final ClassroomGradeGuard guard;
    private final GradeSchemeService schemeService;
    private final LinkedAssessmentSync linkedSync;
    private final GradingContextLoader contextLoader;
    private final GradeCalculator calculator;

    private final GradeComponentRepository componentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final CourseGradeRepository courseGradeRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public GradebookResponse getGradebook(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);
        return refresh(classroom);
    }

    @Override
    @Transactional
    public GradebookResponse saveScores(UUID classroomId, SaveScoresRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        Teacher grader = guard.requireGrader(classroom);
        requireUnlocked(classroomId);

        Map<UUID, Assessment> assessments = new HashMap<>();
        for (Assessment a : assessmentRepository.findByClassroom(classroomId)) {
            assessments.put(a.getAssessmentId(), a);
        }

        Map<UUID, Student> roster = new HashMap<>();
        for (ClassroomStudent cs : classroomStudentRepository.findByClassroom_ClassroomId(classroomId)) {
            if (cs.getStudent() != null) {
                roster.put(cs.getStudent().getStudentId(), cs.getStudent());
            }
        }

        for (SaveScoresRequest.ScoreItem item : request.scores()) {
            Assessment assessment = assessments.get(item.assessmentId());
            if (assessment == null) {
                throw new AssessmentNotFoundException(item.assessmentId());
            }
            if (assessment.isLinked()) {
                throw new InvalidGradingSchemeException(
                        "\"" + assessment.getTitle() + "\" is graded in the "
                                + (assessment.getAssignment() != null ? "assignments" : "quizzes")
                                + " module and cannot be edited here.");
            }

            Student student = roster.get(item.studentId());
            if (student == null) {
                throw new com.universitymanagement.score.exception.StudentNotInClassroomException(
                        item.studentId(), classroomId);
            }

            ScoreStatus status = item.status() != null ? item.status() : ScoreStatus.GRADED;

            if (status == ScoreStatus.GRADED && item.score() != null
                    && item.score() > assessment.getMaxScore()) {
                throw new InvalidGradingSchemeException(
                        "Score " + item.score() + " exceeds the maximum of "
                                + assessment.getMaxScore() + " for \"" + assessment.getTitle() + "\".");
            }

            AssessmentScore existing = scoreRepository
                    .findByAssessment_AssessmentIdAndStudent_StudentId(
                            item.assessmentId(), item.studentId())
                    .orElse(null);

            // A cleared cell means "not graded yet" — the row has to go, so the
            // component stops claiming its weight.
            if (status == ScoreStatus.GRADED && item.score() == null) {
                if (existing != null) {
                    scoreRepository.delete(existing);
                }
                continue;
            }

            AssessmentScore score = existing != null ? existing : new AssessmentScore();
            score.setAssessment(assessment);
            score.setStudent(student);
            score.setScore(status == ScoreStatus.EXCUSED ? null : item.score());
            score.setStatus(status);
            score.setFeedback(item.feedback());
            score.setGradedByTeacher(grader);
            score.setGradedAt(LocalDateTime.now());

            scoreRepository.save(score);
        }

        return refresh(classroom);
    }

    // ---- assessments ----

    @Override
    @Transactional
    public AssessmentResponse createAssessment(UUID classroomId, AssessmentRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);
        requireUnlocked(classroomId);

        GradeComponent component = requireManualComponent(classroomId, request.componentId());

        int nextPosition = assessmentRepository
                .findByComponent_ComponentIdAndIsDeletedFalseOrderByPositionAsc(
                        component.getComponentId())
                .size();

        Assessment assessment = new Assessment();
        assessment.setComponent(component);
        assessment.setTitle(request.title().trim());
        assessment.setMaxScore(request.maxScore());
        assessment.setPosition(request.position() != null ? request.position() : nextPosition);
        assessment.setDueDate(request.dueDate());

        return toAssessmentResponse(assessmentRepository.save(assessment));
    }

    @Override
    @Transactional
    public AssessmentResponse updateAssessment(UUID classroomId, UUID assessmentId,
                                               AssessmentRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);
        requireUnlocked(classroomId);

        Assessment assessment = requireAssessment(classroomId, assessmentId);
        if (assessment.isLinked()) {
            throw new InvalidGradingSchemeException(
                    "\"" + assessment.getTitle() + "\" mirrors another module and cannot be edited here.");
        }

        GradeComponent component = requireManualComponent(classroomId, request.componentId());

        assessment.setComponent(component);
        assessment.setTitle(request.title().trim());
        assessment.setMaxScore(request.maxScore());
        assessment.setDueDate(request.dueDate());
        if (request.position() != null) {
            assessment.setPosition(request.position());
        }

        return toAssessmentResponse(assessmentRepository.save(assessment));
    }

    @Override
    @Transactional
    public void deleteAssessment(UUID classroomId, UUID assessmentId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);
        requireUnlocked(classroomId);

        Assessment assessment = requireAssessment(classroomId, assessmentId);
        if (assessment.isLinked()) {
            throw new InvalidGradingSchemeException(
                    "Remove the underlying assignment or quiz instead — this column mirrors it.");
        }

        // Soft delete keeps the marks recoverable if the column was dropped by mistake.
        assessment.setIsDeleted(true);
        assessmentRepository.save(assessment);
        refresh(classroom);
    }

    // ---- lifecycle ----

    @Override
    @Transactional
    public GradebookResponse submit(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        refresh(classroom);
        requireCompleteScheme(classroomId);

        List<CourseGrade> grades = courseGradeRepository.findByClassroom_ClassroomId(classroomId);
        List<String> incomplete = grades.stream()
                .filter(g -> g.getCompletenessPercent() == null || g.getCompletenessPercent() < 99.99)
                .map(g -> g.getStudent().getStudentCode())
                .filter(Objects::nonNull)
                .toList();

        if (!incomplete.isEmpty()) {
            throw new InvalidGradingSchemeException(
                    "Every component must be marked before submitting. Still incomplete: "
                            + String.join(", ", incomplete.stream().limit(10).toList())
                            + (incomplete.size() > 10 ? " and " + (incomplete.size() - 10) + " more" : ""));
        }

        LocalDateTime now = LocalDateTime.now();
        for (CourseGrade grade : grades) {
            if (grade.getStatus() == CourseGradeStatus.IN_PROGRESS) {
                grade.setStatus(CourseGradeStatus.SUBMITTED);
                grade.setSubmittedAt(now);
            }
        }
        courseGradeRepository.saveAll(grades);

        return build(classroom, contextLoader.load(classroomId));
    }

    @Override
    @Transactional
    public GradebookResponse post(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        if (!guard.isAdmin()) {
            throw new InvalidGradingSchemeException(
                    "Only the registrar can post grades to transcripts.");
        }

        List<CourseGrade> grades = courseGradeRepository.findByClassroom_ClassroomId(classroomId);
        Subject subject = classroom.getSubject();
        Double credit = subject != null ? subject.getCredit() : null;
        LocalDateTime now = LocalDateTime.now();

        for (CourseGrade grade : grades) {
            if (grade.getStatus() != CourseGradeStatus.SUBMITTED) {
                continue;
            }
            grade.setStatus(CourseGradeStatus.POSTED);
            grade.setPostedAt(now);
            grade.setCreditsEarned(
                    grade.getLetterGrade() != null && grade.getLetterGrade().isPassing() && credit != null
                            ? credit
                            : 0.0);
        }
        courseGradeRepository.saveAll(grades);

        return build(classroom, contextLoader.load(classroomId));
    }

    @Override
    @Transactional
    public GradebookResponse reopen(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        if (!guard.isAdmin()) {
            throw new InvalidGradingSchemeException("Only the registrar can reopen posted grades.");
        }

        List<CourseGrade> grades = courseGradeRepository.findByClassroom_ClassroomId(classroomId);
        for (CourseGrade grade : grades) {
            grade.setStatus(CourseGradeStatus.IN_PROGRESS);
            grade.setSubmittedAt(null);
            grade.setPostedAt(null);
            grade.setCreditsEarned(null);
        }
        courseGradeRepository.saveAll(grades);

        return refresh(classroom);
    }

    // ---- recalculation ----

    /**
     * Brings the classroom's stored grades back in line with its marks and
     * returns the rebuilt grid. Posted rows are left alone — a transcript that
     * has been issued does not move.
     */
    private GradebookResponse refresh(Classroom classroom) {
        UUID classroomId = classroom.getClassroomId();

        List<GradeComponent> components = schemeService.ensureScheme(classroom);
        linkedSync.sync(classroomId, components);

        GradingContext context = contextLoader.load(classroomId);

        Map<UUID, CourseGrade> existing = new HashMap<>();
        for (CourseGrade grade : courseGradeRepository.findByClassroom_ClassroomId(classroomId)) {
            existing.put(grade.getStudent().getStudentId(), grade);
        }

        List<CourseGrade> toSave = new ArrayList<>();
        for (Student student : roster(classroomId)) {
            CourseGrade grade = existing.get(student.getStudentId());

            if (grade != null && grade.getStatus() == CourseGradeStatus.POSTED) {
                continue;
            }

            CourseResult result = calculator.calculate(classroomId, student.getStudentId(), context);

            if (grade == null) {
                grade = new CourseGrade();
                grade.setStudent(student);
                grade.setClassroom(classroom);
            }
            grade.setScorePercent(result.scorePercent());
            grade.setCompletenessPercent(result.completenessPercent());
            grade.setLetterGrade(result.letterGrade());
            grade.setGradePoint(result.gradePoint());
            toSave.add(grade);
        }
        courseGradeRepository.saveAll(toSave);

        return build(classroom, context);
    }

    private GradebookResponse build(Classroom classroom, GradingContext context) {
        UUID classroomId = classroom.getClassroomId();

        List<GradeComponent> components = context.componentsOf(classroomId);
        List<GradeComponentResponse> componentResponses = components.stream()
                .map(c -> new GradeComponentResponse(
                        c.getComponentId(),
                        c.getName(),
                        c.getSource(),
                        c.getWeightPercent(),
                        c.getPosition(),
                        c.getSource() == ComponentSource.MANUAL,
                        context.assessmentsOf(c.getComponentId()).stream()
                                .map(this::toAssessmentResponse)
                                .toList()))
                .toList();

        Map<UUID, CourseGrade> grades = new HashMap<>();
        for (CourseGrade grade : courseGradeRepository.findByClassroom_ClassroomId(classroomId)) {
            grades.put(grade.getStudent().getStudentId(), grade);
        }

        List<UUID> assessmentIds = components.stream()
                .flatMap(c -> context.assessmentsOf(c.getComponentId()).stream())
                .map(Assessment::getAssessmentId)
                .toList();

        List<GradebookResponse.StudentRow> rows = new ArrayList<>();
        for (Student student : roster(classroomId)) {
            UUID studentId = student.getStudentId();
            CourseResult result = calculator.calculate(classroomId, studentId, context);
            CourseGrade stored = grades.get(studentId);

            List<GradebookResponse.Cell> cells = new ArrayList<>();
            for (UUID assessmentId : assessmentIds) {
                AssessmentScore score = context.scoreOf(assessmentId, studentId);
                if (score != null) {
                    cells.add(new GradebookResponse.Cell(
                            assessmentId, score.getScore(), score.getStatus()));
                }
            }

            // A posted grade is what the transcript says, even if later edits to
            // the marks would now compute something different.
            boolean posted = stored != null && stored.getStatus() == CourseGradeStatus.POSTED;
            Double percent = posted ? stored.getScorePercent() : result.scorePercent();
            LetterGrade letter = posted ? stored.getLetterGrade() : result.letterGrade();
            Double point = posted ? stored.getGradePoint() : result.gradePoint();

            rows.add(new GradebookResponse.StudentRow(
                    studentId,
                    student.getStudentCode(),
                    student.getUser() != null ? student.getUser().getFullName() : null,
                    avatarUrlOf(student),
                    cells,
                    result.components().stream().map(this::toBreakdown).toList(),
                    percent,
                    posted ? stored.getCompletenessPercent() : result.completenessPercent(),
                    letter != null ? letter.getDisplay() : null,
                    point,
                    stored != null ? stored.getStatus() : CourseGradeStatus.IN_PROGRESS));
        }

        double totalWeight = components.stream()
                .mapToDouble(c -> c.getWeightPercent() != null ? c.getWeightPercent() : 0.0)
                .sum();

        CourseGradeStatus status = grades.values().stream()
                .map(CourseGrade::getStatus)
                .min(Comparator.comparingInt(Enum::ordinal))
                .orElse(CourseGradeStatus.IN_PROGRESS);

        Subject subject = classroom.getSubject();

        return new GradebookResponse(
                classroomId,
                classroom.getClassName(),
                subject != null ? subject.getSubjectCode() : null,
                subject != null ? subject.getSubjectName() : null,
                subject != null ? subject.getCredit() : null,
                classroom.getAcademicYear(),
                classroom.getSemester(),
                Math.round(totalWeight * 100.0) / 100.0,
                Math.abs(totalWeight - 100.0) <= SCHEME_TOLERANCE,
                status,
                status != CourseGradeStatus.IN_PROGRESS,
                componentResponses,
                rows);
    }

    // ---- helpers ----

    /**
     * Signing is local work, so doing it per row costs nothing measurable — but
     * a storage hiccup must not take the whole gradebook down with it.
     */
    private String avatarUrlOf(Student student) {
        if (student.getUser() == null || student.getUser().getAvatarObjectName() == null) {
            return null;
        }
        try {
            return minioService.getAssetPreviewUrl(student.getUser().getAvatarObjectName());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Student> roster(UUID classroomId) {
        return classroomStudentRepository.findByClassroom_ClassroomId(classroomId).stream()
                .map(ClassroomStudent::getStudent)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        s -> s.getUser() != null && s.getUser().getFullName() != null
                                ? s.getUser().getFullName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void requireUnlocked(UUID classroomId) {
        courseGradeRepository.findByClassroom_ClassroomId(classroomId).stream()
                .map(CourseGrade::getStatus)
                .filter(s -> s != CourseGradeStatus.IN_PROGRESS)
                .findFirst()
                .ifPresent(status -> {
                    throw new GradeLockedException(classroomId, status);
                });
    }

    private void requireCompleteScheme(UUID classroomId) {
        double total = componentRepository
                .findByClassroom_ClassroomIdOrderByPositionAsc(classroomId).stream()
                .mapToDouble(c -> c.getWeightPercent() != null ? c.getWeightPercent() : 0.0)
                .sum();

        if (Math.abs(total - 100.0) > SCHEME_TOLERANCE) {
            throw new InvalidGradingSchemeException(
                    "The grading policy totals " + Math.round(total * 100.0) / 100.0
                            + "% and must total 100% before grades can be submitted.");
        }
    }

    private GradeComponent requireManualComponent(UUID classroomId, UUID componentId) {
        GradeComponent component = componentRepository.findById(componentId)
                .filter(c -> c.getClassroom().getClassroomId().equals(classroomId))
                .orElseThrow(() -> new GradeComponentNotFoundException(componentId));

        if (component.getSource() != ComponentSource.MANUAL) {
            throw new InvalidGradingSchemeException(
                    "\"" + component.getName() + "\" is filled in automatically from the "
                            + component.getSource().name().toLowerCase()
                            + " records, so columns cannot be added to it by hand.");
        }
        return component;
    }

    private Assessment requireAssessment(UUID classroomId, UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .filter(a -> a.getComponent().getClassroom().getClassroomId().equals(classroomId))
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }

    private AssessmentResponse toAssessmentResponse(Assessment a) {
        String linkedTo = a.getAssignment() != null ? "ASSIGNMENT"
                : a.getQuiz() != null ? "QUIZ" : null;
        UUID linkedId = a.getAssignment() != null ? a.getAssignment().getAssignmentId()
                : a.getQuiz() != null ? a.getQuiz().getQuizId() : null;

        return new AssessmentResponse(
                a.getAssessmentId(),
                a.getComponent().getComponentId(),
                a.getTitle(),
                a.getMaxScore(),
                a.getPosition(),
                a.getDueDate(),
                linkedTo,
                linkedId,
                !a.isLinked());
    }

    private GradebookResponse.ComponentBreakdown toBreakdown(ComponentResult r) {
        return new GradebookResponse.ComponentBreakdown(
                r.componentId(),
                r.name(),
                r.source(),
                r.weightPercent(),
                r.percent(),
                r.earnedPoints(),
                r.possiblePoints(),
                r.gradedItems(),
                r.totalItems());
    }
}
