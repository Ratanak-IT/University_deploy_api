package com.universitymanagement.grading;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.grading.calc.AttendanceTally;
import com.universitymanagement.grading.calc.ComponentResult;
import com.universitymanagement.grading.calc.CourseResult;
import com.universitymanagement.grading.calc.GradeCalculator;
import com.universitymanagement.grading.calc.GradingContext;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.entity.LetterGrade;
import com.universitymanagement.grading.entity.ScoreStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeCalculatorTest {

    private final GradeCalculator calculator = new GradeCalculator();
    private final UUID classroomId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @Test
    void weightsEachComponentByItsShareOfTheFinalGrade() {
        Fixture fixture = new Fixture();
        GradeComponent midterm = fixture.component("Midterm", ComponentSource.MANUAL, 30);
        GradeComponent finalExam = fixture.component("Final Exam", ComponentSource.MANUAL, 70);

        fixture.score(fixture.assessment(midterm, 100), 80);
        fixture.score(fixture.assessment(finalExam, 100), 60);

        CourseResult result = calculator.calculate(classroomId, studentId, fixture.build());

        // 80 * 0.3 + 60 * 0.7
        assertEquals(66.0, result.scorePercent());
        assertEquals(100.0, result.completenessPercent());
        assertEquals(LetterGrade.C_PLUS, result.letterGrade());
    }

    @Test
    void poolsPointsWithinAComponentSoABigExamOutweighsASmallQuiz() {
        Fixture fixture = new Fixture();
        GradeComponent quizzes = fixture.component("Quizzes", ComponentSource.MANUAL, 100);

        fixture.score(fixture.assessment(quizzes, 10), 10);   // perfect, small
        fixture.score(fixture.assessment(quizzes, 90), 45);   // half, large

        CourseResult result = calculator.calculate(classroomId, studentId, fixture.build());

        // 55 earned of 100 possible — not the 75% a naive average would give.
        assertEquals(55.0, result.scorePercent());
    }

    @Test
    void reportsStandingWithoutClaimingTheCourseIsFinished() {
        Fixture fixture = new Fixture();
        GradeComponent midterm = fixture.component("Midterm", ComponentSource.MANUAL, 30);
        GradeComponent finalExam = fixture.component("Final Exam", ComponentSource.MANUAL, 70);

        fixture.score(fixture.assessment(midterm, 100), 80);
        fixture.assessment(finalExam, 100); // not sat yet

        CourseResult result = calculator.calculate(classroomId, studentId, fixture.build());

        assertEquals(80.0, result.scorePercent());
        assertEquals(30.0, result.completenessPercent());
        assertTrue(!result.isComplete(),
                "a course graded on the midterm alone must not read as complete");
    }

    @Test
    void anUngradedItemIsNotAZero() {
        Fixture fixture = new Fixture();
        GradeComponent homework = fixture.component("Homework", ComponentSource.MANUAL, 100);

        fixture.score(fixture.assessment(homework, 100), 90);
        fixture.assessment(homework, 100); // second piece not marked yet

        CourseResult result = calculator.calculate(classroomId, studentId, fixture.build());

        assertEquals(90.0, result.scorePercent(),
                "the unmarked item must stay out of the denominator");
    }

    @Test
    void missingCountsAsZeroAndExcusedDropsOut() {
        Fixture missing = new Fixture();
        GradeComponent c1 = missing.component("Homework", ComponentSource.MANUAL, 100);
        missing.score(missing.assessment(c1, 100), 90);
        missing.status(missing.assessment(c1, 100), ScoreStatus.MISSING);

        assertEquals(45.0, calculator.calculate(classroomId, studentId, missing.build()).scorePercent());

        Fixture excused = new Fixture();
        GradeComponent c2 = excused.component("Homework", ComponentSource.MANUAL, 100);
        excused.score(excused.assessment(c2, 100), 90);
        excused.status(excused.assessment(c2, 100), ScoreStatus.EXCUSED);

        assertEquals(90.0, calculator.calculate(classroomId, studentId, excused.build()).scorePercent());
    }

    @Test
    void attendanceScoresTheCreditItWasGiven() {
        Fixture fixture = new Fixture();
        fixture.component("Attendance", ComponentSource.ATTENDANCE, 100);
        // 8 present + 2 late at half credit = 9 earned, over 12 counted
        // sessions; the excused ones already left the denominator upstream.
        fixture.attendance(new AttendanceTally(9, 12));

        CourseResult result = calculator.calculate(classroomId, studentId, fixture.build());

        assertEquals(75.0, result.scorePercent());
    }

    @Test
    void aCourseWithNoMarksAtAllHasNoGrade() {
        Fixture fixture = new Fixture();
        GradeComponent midterm = fixture.component("Midterm", ComponentSource.MANUAL, 100);
        fixture.assessment(midterm, 100);

        CourseResult result = calculator.calculate(classroomId, studentId, fixture.build());

        assertNull(result.scorePercent());
        assertNull(result.letterGrade());
        assertEquals(0.0, result.completenessPercent());
    }

    @Test
    void letterGradeBoundariesAreContiguous() {
        assertEquals(LetterGrade.A, LetterGrade.fromPercent(90.0));
        assertEquals(LetterGrade.A_MINUS, LetterGrade.fromPercent(89.99));
        assertEquals(LetterGrade.D, LetterGrade.fromPercent(50.0));
        assertEquals(LetterGrade.F, LetterGrade.fromPercent(49.99));
        assertEquals(LetterGrade.F, LetterGrade.fromPercent(0.0));

        // Every percentage lands on exactly one letter, with points that never
        // increase as the score falls.
        double previousPoint = Double.MAX_VALUE;
        for (int percent = 100; percent >= 0; percent--) {
            LetterGrade grade = LetterGrade.fromPercent(percent);
            assertTrue(grade.getGradePoint() <= previousPoint,
                    "grade points must not rise as the percentage falls, at " + percent);
            previousPoint = grade.getGradePoint();
        }
    }

    @Test
    void breakdownKeepsUnmarkedComponentsVisible() {
        Fixture fixture = new Fixture();
        GradeComponent midterm = fixture.component("Midterm", ComponentSource.MANUAL, 40);
        fixture.component("Final Exam", ComponentSource.MANUAL, 60);
        fixture.score(fixture.assessment(midterm, 100), 70);

        List<ComponentResult> breakdown =
                calculator.calculate(classroomId, studentId, fixture.build()).components();

        assertEquals(2, breakdown.size());
        assertEquals(70.0, breakdown.getFirst().percent());
        assertNull(breakdown.get(1).percent(),
                "an unmarked component must report null, not zero");
    }

    /** Builds a {@link GradingContext} without touching the database. */
    private final class Fixture {
        private final List<GradeComponent> components = new ArrayList<>();
        private final Map<UUID, List<Assessment>> assessments = new HashMap<>();
        private final Map<UUID, Map<UUID, AssessmentScore>> scores = new HashMap<>();
        private AttendanceTally tally = AttendanceTally.empty();

        GradeComponent component(String name, ComponentSource source, double weight) {
            Classroom classroom = new Classroom();
            classroom.setClassroomId(classroomId);

            GradeComponent component = new GradeComponent();
            component.setComponentId(UUID.randomUUID());
            component.setClassroom(classroom);
            component.setName(name);
            component.setSource(source);
            component.setWeightPercent(weight);
            component.setPosition(components.size());

            components.add(component);
            assessments.put(component.getComponentId(), new ArrayList<>());
            return component;
        }

        Assessment assessment(GradeComponent component, double maxScore) {
            Assessment assessment = new Assessment();
            assessment.setAssessmentId(UUID.randomUUID());
            assessment.setComponent(component);
            assessment.setMaxScore(maxScore);
            assessment.setPosition(assessments.get(component.getComponentId()).size());

            assessments.get(component.getComponentId()).add(assessment);
            return assessment;
        }

        void score(Assessment assessment, double value) {
            AssessmentScore score = new AssessmentScore();
            score.setScore(value);
            score.setStatus(ScoreStatus.GRADED);
            put(assessment, score);
        }

        void status(Assessment assessment, ScoreStatus status) {
            AssessmentScore score = new AssessmentScore();
            score.setStatus(status);
            put(assessment, score);
        }

        void attendance(AttendanceTally value) {
            this.tally = value;
        }

        private void put(Assessment assessment, AssessmentScore score) {
            scores.computeIfAbsent(assessment.getAssessmentId(), k -> new HashMap<>())
                    .put(studentId, score);
        }

        GradingContext build() {
            return new GradingContext(
                    Map.of(classroomId, components),
                    assessments,
                    scores,
                    Map.of(classroomId, Map.of(studentId, tally)));
        }
    }
}
