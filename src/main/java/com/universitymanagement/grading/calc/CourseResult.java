package com.universitymanagement.grading.calc;

import com.universitymanagement.grading.entity.LetterGrade;

import java.util.List;

/**
 * A student's standing in one course.
 *
 * @param scorePercent        weighted percentage over the components that carry
 *                            marks — the student's standing *so far*, not a
 *                            prediction of the final grade
 * @param completenessPercent how much of the policy weight has been marked;
 *                            only at 100 is {@code scorePercent} a final grade
 */
public record CourseResult(
        Double scorePercent,
        Double completenessPercent,
        LetterGrade letterGrade,
        Double gradePoint,
        List<ComponentResult> components
) {
    public boolean isComplete() {
        return completenessPercent != null && completenessPercent >= 99.99;
    }

    public static CourseResult empty(List<ComponentResult> components) {
        return new CourseResult(null, 0.0, null, null, components);
    }
}
