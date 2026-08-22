package com.universitymanagement.grading.service.impl;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.dto.request.SaveGradeSchemeRequest;
import com.universitymanagement.grading.dto.response.AssessmentResponse;
import com.universitymanagement.grading.dto.response.GradeComponentResponse;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.exception.GradeComponentNotFoundException;
import com.universitymanagement.grading.exception.InvalidGradingSchemeException;
import com.universitymanagement.grading.repository.AssessmentRepository;
import com.universitymanagement.grading.repository.GradeComponentRepository;
import com.universitymanagement.grading.security.ClassroomGradeGuard;
import com.universitymanagement.grading.service.GradeSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeSchemeServiceImpl implements GradeSchemeService {

    /** Total the weights of a classroom's components must reach. */
    private static final double REQUIRED_TOTAL = 100.0;

    /** Floating-point slack, so 33.33 + 33.33 + 33.34 is accepted. */
    private static final double TOLERANCE = 0.01;

    /**
     * Seeded for a classroom that has never been graded. It is a starting point,
     * not a rule — a teacher can replace it entirely, as long as it still totals 100.
     */
    private static final List<DefaultComponent> DEFAULT_SCHEME = List.of(
            new DefaultComponent("Midterm", ComponentSource.MANUAL, 25.0),
            new DefaultComponent("Final Exam", ComponentSource.MANUAL, 40.0),
            new DefaultComponent("Assignments", ComponentSource.ASSIGNMENT, 15.0),
            new DefaultComponent("Quizzes", ComponentSource.QUIZ, 10.0),
            new DefaultComponent("Attendance", ComponentSource.ATTENDANCE, 10.0));

    private record DefaultComponent(String name, ComponentSource source, double weight) {
    }

    private final GradeComponentRepository componentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassroomGradeGuard guard;

    @Override
    @Transactional
    public List<GradeComponentResponse> getScheme(UUID classroomId) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireReader(classroom);
        return ensureScheme(classroom).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<GradeComponentResponse> saveScheme(UUID classroomId, SaveGradeSchemeRequest request) {
        Classroom classroom = guard.requireClassroom(classroomId);
        guard.requireGrader(classroom);

        validate(request.components());

        List<GradeComponent> existing =
                componentRepository.findByClassroom_ClassroomIdOrderByPositionAsc(classroomId);
        Set<UUID> keptIds = new HashSet<>();
        List<GradeComponent> saved = new ArrayList<>();

        int position = 0;
        for (SaveGradeSchemeRequest.ComponentItem item : request.components()) {
            GradeComponent component;
            if (item.componentId() != null) {
                component = existing.stream()
                        .filter(c -> c.getComponentId().equals(item.componentId()))
                        .findFirst()
                        .orElseThrow(() -> new GradeComponentNotFoundException(item.componentId()));
            } else {
                component = new GradeComponent();
                component.setClassroom(classroom);
            }

            component.setName(item.name().trim());
            component.setSource(item.source());
            component.setWeightPercent(item.weightPercent());
            component.setPosition(item.position() != null ? item.position() : position);

            GradeComponent persisted = componentRepository.save(component);
            keptIds.add(persisted.getComponentId());
            saved.add(persisted);
            position++;
        }

        // Dropping a component discards its columns and marks with it, which is
        // why the request has to name every component the classroom should keep.
        List<GradeComponent> removed = existing.stream()
                .filter(c -> !keptIds.contains(c.getComponentId()))
                .toList();
        if (!removed.isEmpty()) {
            componentRepository.deleteAll(removed);
        }

        saved.sort(Comparator.comparing(GradeComponent::getPosition));
        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<GradeComponent> ensureScheme(Classroom classroom) {
        List<GradeComponent> existing = componentRepository
                .findByClassroom_ClassroomIdOrderByPositionAsc(classroom.getClassroomId());
        if (!existing.isEmpty()) {
            return existing;
        }

        List<GradeComponent> created = new ArrayList<>();
        int position = 0;
        for (DefaultComponent template : DEFAULT_SCHEME) {
            GradeComponent component = new GradeComponent();
            component.setClassroom(classroom);
            component.setName(template.name());
            component.setSource(template.source());
            component.setWeightPercent(template.weight());
            component.setPosition(position++);
            created.add(componentRepository.save(component));
        }

        // Manual components would otherwise open with no column to type into.
        for (GradeComponent component : created) {
            if (component.getSource() == ComponentSource.MANUAL) {
                Assessment assessment = new Assessment();
                assessment.setComponent(component);
                assessment.setTitle(component.getName());
                assessment.setMaxScore(100.0);
                assessment.setPosition(0);
                assessmentRepository.save(assessment);
            }
        }

        return componentRepository
                .findByClassroom_ClassroomIdOrderByPositionAsc(classroom.getClassroomId());
    }

    private void validate(List<SaveGradeSchemeRequest.ComponentItem> items) {
        double total = items.stream()
                .mapToDouble(i -> i.weightPercent() != null ? i.weightPercent() : 0.0)
                .sum();

        if (Math.abs(total - REQUIRED_TOTAL) > TOLERANCE) {
            throw new InvalidGradingSchemeException(
                    "Component weights must total 100%, but they total "
                            + Math.round(total * 100.0) / 100.0 + "%.");
        }

        Set<String> names = new HashSet<>();
        for (SaveGradeSchemeRequest.ComponentItem item : items) {
            String key = item.name().trim().toLowerCase(Locale.ROOT);
            if (!names.add(key)) {
                throw new InvalidGradingSchemeException(
                        "Duplicate component name: " + item.name().trim());
            }
        }

        long derived = items.stream()
                .filter(i -> i.source() == ComponentSource.ATTENDANCE)
                .count();
        if (derived > 1) {
            throw new InvalidGradingSchemeException(
                    "A classroom can have at most one attendance component.");
        }
    }

    private GradeComponentResponse toResponse(GradeComponent component) {
        boolean editable = component.getSource() == ComponentSource.MANUAL;

        List<AssessmentResponse> assessments = component.getSource() == ComponentSource.ATTENDANCE
                ? List.of()
                : assessmentRepository
                .findByComponent_ComponentIdAndIsDeletedFalseOrderByPositionAsc(
                        component.getComponentId())
                .stream()
                .map(a -> toAssessmentResponse(a, component))
                .toList();

        return new GradeComponentResponse(
                component.getComponentId(),
                component.getName(),
                component.getSource(),
                component.getWeightPercent(),
                component.getPosition(),
                editable,
                assessments);
    }

    private AssessmentResponse toAssessmentResponse(Assessment a, GradeComponent component) {
        String linkedTo = a.getAssignment() != null ? "ASSIGNMENT"
                : a.getQuiz() != null ? "QUIZ" : null;
        UUID linkedId = a.getAssignment() != null ? a.getAssignment().getAssignmentId()
                : a.getQuiz() != null ? a.getQuiz().getQuizId() : null;

        return new AssessmentResponse(
                a.getAssessmentId(),
                component.getComponentId(),
                a.getTitle(),
                a.getMaxScore(),
                a.getPosition(),
                a.getDueDate(),
                linkedTo,
                linkedId,
                !a.isLinked());
    }
}
