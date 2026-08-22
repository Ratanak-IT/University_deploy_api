package com.universitymanagement.grading.migration;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.entity.ScoreStatus;
import com.universitymanagement.grading.repository.AssessmentRepository;
import com.universitymanagement.grading.repository.AssessmentScoreRepository;
import com.universitymanagement.grading.service.GradeSchemeService;
import com.universitymanagement.score.entity.ExamScore;
import com.universitymanagement.score.entity.ExamType;
import com.universitymanagement.score.repository.ExamScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Copies scores from the old flat {@code exam_scores} table into the gradebook,
 * once, on the first start after the new model ships.
 *
 * <p>Only the two types that were genuinely typed by hand are carried over.
 * Assignment, quiz and attendance rows are deliberately dropped: the gradebook
 * derives those from the modules that own them, and importing the old copies
 * would recreate the double-counting the new model exists to prevent.
 */
@Component
@RequiredArgsConstructor
@Order(100)
@Slf4j
public class LegacyExamScoreMigration implements ApplicationRunner {

    private static final Map<ExamType, String> MIGRATED_TYPES = Map.of(
            ExamType.MIDTERM, "Midterm",
            ExamType.FINAL, "Final Exam");

    private final ExamScoreRepository examScoreRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final GradeSchemeService schemeService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (scoreRepository.count() > 0) {
            return; // already migrated, or the gradebook is in use
        }

        List<ExamScore> legacy = examScoreRepository.findAll();
        if (legacy.isEmpty()) {
            return;
        }

        Map<UUID, Map<String, Assessment>> targets = new HashMap<>();
        int migrated = 0;
        int skipped = 0;

        for (ExamScore old : legacy) {
            String componentName = MIGRATED_TYPES.get(old.getExamType());
            if (componentName == null || old.getScore() == null
                    || old.getMaxScore() == null || old.getMaxScore() <= 0
                    || old.getClassroom() == null || old.getStudent() == null) {
                skipped++;
                continue;
            }

            Classroom classroom = old.getClassroom();
            Assessment assessment = targets
                    .computeIfAbsent(classroom.getClassroomId(), k -> new HashMap<>())
                    .computeIfAbsent(componentName,
                            name -> assessmentFor(classroom, name, old.getMaxScore()));

            if (assessment == null) {
                skipped++;
                continue;
            }

            AssessmentScore score = new AssessmentScore();
            score.setAssessment(assessment);
            score.setStudent(old.getStudent());
            score.setScore(old.getScore());
            score.setStatus(ScoreStatus.GRADED);
            score.setGradedByTeacher(old.getEnteredByTeacher());
            score.setGradedAt(old.getLastUpdateAt() != null ? old.getLastUpdateAt() : old.getCreatedAt());

            scoreRepository.save(score);
            migrated++;
        }

        log.info("Legacy exam score migration: {} carried over, {} skipped "
                + "(assignment/quiz/attendance scores are now derived from their own modules).",
                migrated, skipped);
    }

    private Assessment assessmentFor(Classroom classroom, String componentName, Double maxScore) {
        GradeComponent component = schemeService.ensureScheme(classroom).stream()
                .filter(c -> c.getSource() == ComponentSource.MANUAL)
                .filter(c -> c.getName().equalsIgnoreCase(componentName))
                .findFirst()
                .orElse(null);

        if (component == null) {
            return null;
        }

        Assessment assessment = assessmentRepository
                .findByComponent_ComponentIdAndIsDeletedFalseOrderByPositionAsc(
                        component.getComponentId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Assessment fresh = new Assessment();
                    fresh.setComponent(component);
                    fresh.setTitle(component.getName());
                    fresh.setPosition(0);
                    return fresh;
                });

        assessment.setMaxScore(maxScore);
        return assessmentRepository.save(assessment);
    }
}
