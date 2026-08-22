package com.universitymanagement.grading.calc;

import com.universitymanagement.grading.dto.response.CourseGradeResponse;
import com.universitymanagement.grading.entity.CourseGrade;
import com.universitymanagement.grading.entity.CourseGradeStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Credit-weighted GPA.
 *
 * <p>Two figures come out of this, and conflating them is what made the old
 * numbers untrustworthy:
 *
 * <ul>
 *   <li><b>Cumulative</b> counts only posted grades. It is the official figure,
 *       and it does not move because a teacher edited a mark last night.
 *   <li><b>Current</b> also folds in courses still being marked, so a student
 *       can see where they stand mid-term. It is an estimate and is labelled
 *       as one in the UI.
 * </ul>
 *
 * <p>Grades carrying an administrative mark (W, I, P/NP) or belonging to a
 * superseded retake are excluded from both.
 */
@Component
public class GpaCalculator {

    public record Gpa(Double cumulativeGpa, Double currentGpa, Double creditsEarned,
                      Double creditsAttempted) {
    }

    /**
     * Same arithmetic straight off the entities.
     *
     * <p>A cohort listing needs a GPA per student and nothing else; building
     * the full response DTO — component breakdowns included — for every row
     * just to divide two numbers would be wasted work.
     */
    public Gpa fromEntities(List<CourseGrade> grades) {
        return reduce(grades.stream()
                .map(g -> new Line(
                        Boolean.TRUE.equals(g.getCountsInGpa()),
                        g.getGradePoint(),
                        g.getClassroom() != null && g.getClassroom().getSubject() != null
                                ? g.getClassroom().getSubject().getCredit() : null,
                        g.getStatus(),
                        g.getCreditsEarned()))
                .toList());
    }

    public Gpa calculate(List<CourseGradeResponse> grades) {
        return reduce(grades.stream()
                .map(g -> new Line(
                        Boolean.TRUE.equals(g.countsInGpa()),
                        g.gradePoint(),
                        g.credit(),
                        g.status(),
                        g.creditsEarned()))
                .toList());
    }

    /** The handful of fields the arithmetic actually reads. */
    private record Line(boolean countsInGpa, Double gradePoint, Double credit,
                        CourseGradeStatus status, Double creditsEarned) {
    }

    private Gpa reduce(List<Line> grades) {
        double postedPoints = 0.0;
        double postedCredits = 0.0;
        double livePoints = 0.0;
        double liveCredits = 0.0;
        double earned = 0.0;

        for (Line grade : grades) {
            if (!grade.countsInGpa() || grade.gradePoint() == null) {
                continue;
            }
            double credit = grade.credit() != null ? grade.credit() : 1.0;

            livePoints += grade.gradePoint() * credit;
            liveCredits += credit;

            if (grade.status() == CourseGradeStatus.POSTED) {
                postedPoints += grade.gradePoint() * credit;
                postedCredits += credit;
                earned += grade.creditsEarned() != null ? grade.creditsEarned() : 0.0;
            }
        }

        // Credits report absence the same way the GPA does. Returning 0.0 for a
        // student whose grades are all still unposted reads as "earned nothing",
        // which is a different — and much worse — claim than "nothing official
        // yet", and it is the one a transcript must not make by accident.
        return new Gpa(
                postedCredits > 0 ? round(postedPoints / postedCredits) : null,
                liveCredits > 0 ? round(livePoints / liveCredits) : null,
                postedCredits > 0 ? round(earned) : null,
                postedCredits > 0 ? round(postedCredits) : null);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
