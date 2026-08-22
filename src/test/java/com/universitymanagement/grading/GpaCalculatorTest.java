package com.universitymanagement.grading;

import com.universitymanagement.grading.calc.GpaCalculator;
import com.universitymanagement.grading.dto.response.CourseGradeResponse;
import com.universitymanagement.grading.entity.CourseGradeStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GpaCalculatorTest {

    private final GpaCalculator calculator = new GpaCalculator();

    private CourseGradeResponse grade(double gradePoint, double credit,
                                      CourseGradeStatus status, Double creditsEarned) {
        return new CourseGradeResponse(
                UUID.randomUUID(), UUID.randomUUID(), "STU-001", "Sok Dara",
                UUID.randomUUID(), "CS301-A", UUID.randomUUID(), "CS301", "Database Systems",
                credit, "2025-2026", 1,
                85.0, "A-", gradePoint, creditsEarned, 100.0,
                status, true, null, null, List.of());
    }

    @Test
    void weightsGradePointsByCredit() {
        List<CourseGradeResponse> grades = List.of(
                grade(4.0, 3.0, CourseGradeStatus.POSTED, 3.0),
                grade(2.0, 1.0, CourseGradeStatus.POSTED, 1.0));

        // (4*3 + 2*1) / 4
        assertEquals(3.5, calculator.calculate(grades).cumulativeGpa());
    }

    @Test
    void theOfficialFigureCountsPostedGradesOnly() {
        List<CourseGradeResponse> grades = List.of(
                grade(4.0, 3.0, CourseGradeStatus.POSTED, 3.0),
                grade(1.0, 3.0, CourseGradeStatus.IN_PROGRESS, null));

        GpaCalculator.Gpa gpa = calculator.calculate(grades);

        assertEquals(4.0, gpa.cumulativeGpa(), "the unposted course must not drag it down");
        assertEquals(2.5, gpa.currentGpa(), "the live figure includes it");
    }

    @Test
    void nothingPostedReportsNoCreditsRatherThanZero() {
        List<CourseGradeResponse> grades = List.of(
                grade(3.0, 3.0, CourseGradeStatus.IN_PROGRESS, null),
                grade(3.5, 3.0, CourseGradeStatus.SUBMITTED, null));

        GpaCalculator.Gpa gpa = calculator.calculate(grades);

        assertNull(gpa.cumulativeGpa());
        assertNull(gpa.creditsEarned(),
                "0.0 would read as 'earned nothing' instead of 'nothing posted yet'");
        assertNull(gpa.creditsAttempted());
        assertEquals(3.25, gpa.currentGpa(), "the provisional figure is still available");
    }

    @Test
    void aFailedCourseEarnsNoCreditButStillCountsInTheGpa() {
        List<CourseGradeResponse> grades = List.of(
                grade(4.0, 3.0, CourseGradeStatus.POSTED, 3.0),
                grade(0.0, 3.0, CourseGradeStatus.POSTED, 0.0));

        GpaCalculator.Gpa gpa = calculator.calculate(grades);

        assertEquals(2.0, gpa.cumulativeGpa(), "the F is averaged in");
        assertEquals(3.0, gpa.creditsEarned(), "but earns no credit");
        assertEquals(6.0, gpa.creditsAttempted(), "while still counting as attempted");
    }

    @Test
    void aStudentWithNoGradesAtAllHasNoFigures() {
        GpaCalculator.Gpa gpa = calculator.calculate(List.of());

        assertNull(gpa.cumulativeGpa());
        assertNull(gpa.currentGpa());
        assertNull(gpa.creditsEarned());
        assertNull(gpa.creditsAttempted());
    }
}
