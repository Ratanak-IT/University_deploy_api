package com.universitymanagement.grading.calc;

import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.entity.LetterGrade;
import com.universitymanagement.grading.entity.ScoreStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns marks into a course grade, following the classroom's published policy.
 *
 * <p>Two rules drive everything here and are worth stating plainly:
 *
 * <ul>
 *   <li>Each component is scored on <em>points</em> — total earned over total
 *       possible across its assessments — before its weight is applied. That is
 *       what makes a 10-point quiz count less than a 100-point exam inside the
 *       same component.
 *   <li>Weight is only claimed by components that actually have marks. A course
 *       graded on midterm alone reports the midterm percentage at 30%
 *       completeness, never as a finished grade.
 * </ul>
 */
@Component
public class GradeCalculator {

    public CourseResult calculate(UUID classroomId, UUID studentId, GradingContext context) {
        List<GradeComponent> components = context.componentsOf(classroomId);
        List<ComponentResult> results = new ArrayList<>();

        for (GradeComponent component : components) {
            results.add(component.getSource() == ComponentSource.ATTENDANCE
                    ? scoreAttendance(component, classroomId, studentId, context)
                    : scoreAssessments(component, studentId, context));
        }

        double totalWeight = results.stream().mapToDouble(ComponentResult::weightPercent).sum();
        if (totalWeight <= 0) {
            return CourseResult.empty(results);
        }

        double gradedWeight = results.stream()
                .filter(ComponentResult::hasData)
                .mapToDouble(ComponentResult::weightPercent)
                .sum();

        if (gradedWeight <= 0) {
            return CourseResult.empty(results);
        }

        double weighted = results.stream()
                .filter(ComponentResult::hasData)
                .mapToDouble(r -> r.percent() * r.weightPercent())
                .sum();

        double percent = round(weighted / gradedWeight);
        LetterGrade letter = LetterGrade.fromPercent(percent);

        return new CourseResult(
                percent,
                round(gradedWeight / totalWeight * 100.0),
                letter,
                letter.getGradePoint(),
                results);
    }

    private ComponentResult scoreAssessments(GradeComponent component, UUID studentId,
                                             GradingContext context) {
        List<Assessment> assessments = context.assessmentsOf(component.getComponentId());

        double earned = 0.0;
        double possible = 0.0;
        int graded = 0;

        for (Assessment assessment : assessments) {
            AssessmentScore score = context.scoreOf(assessment.getAssessmentId(), studentId);

            // No row at all means not marked yet: it must not drag the average
            // down, and it must not let the course look finished either.
            if (score == null || score.getStatus() == ScoreStatus.EXCUSED) {
                continue;
            }

            double max = assessment.getMaxScore() != null ? assessment.getMaxScore() : 0.0;
            if (max <= 0) {
                continue;
            }

            earned += score.getStatus() == ScoreStatus.MISSING || score.getScore() == null
                    ? 0.0
                    : score.getScore();
            possible += max;
            graded++;
        }

        return new ComponentResult(
                component.getComponentId(),
                component.getName(),
                component.getSource(),
                component.getWeightPercent() != null ? component.getWeightPercent() : 0.0,
                possible > 0 ? round(earned / possible * 100.0) : null,
                earned,
                possible,
                graded,
                assessments.size());
    }

    private ComponentResult scoreAttendance(GradeComponent component, UUID classroomId,
                                            UUID studentId, GradingContext context) {
        AttendanceTally tally = context.attendanceOf(classroomId, studentId);

        return new ComponentResult(
                component.getComponentId(),
                component.getName(),
                component.getSource(),
                component.getWeightPercent() != null ? component.getWeightPercent() : 0.0,
                tally.hasData() ? round(tally.earned() / tally.possible() * 100.0) : null,
                tally.earned(),
                tally.possible(),
                (int) tally.possible(),
                (int) tally.possible());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
