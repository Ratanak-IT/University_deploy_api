package com.universitymanagement.student.service.impl;

import com.universitymanagement.curriculum.repository.CurriculumRepository;
import com.universitymanagement.grading.calc.GpaCalculator;
import com.universitymanagement.grading.entity.AcademicStanding;
import com.universitymanagement.grading.entity.CourseGrade;
import com.universitymanagement.grading.entity.CourseGradeStatus;
import com.universitymanagement.grading.repository.CourseGradeRepository;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.student.dto.response.StudentAcademicSummaryResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.student.service.StudentCohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The transcript list: one row per student, covering their whole record.
 *
 * <p>The screen used to require picking a single student before it showed
 * anything, which made it useless for the question a registrar actually brings
 * to it — who is below the line, and who is ready to graduate. Answering that
 * needs the cohort, so this loads it in a fixed four queries no matter how many
 * students are in it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCohortServiceImpl implements StudentCohortService {

    private final StudentRepository studentRepository;
    private final CourseGradeRepository courseGradeRepository;
    private final CurriculumRepository curriculumRepository;
    private final GpaCalculator gpaCalculator;
    private final MinioService minioService;

    @Override
    public List<StudentAcademicSummaryResponse> getCohort(UUID programId, Integer yearLevel,
                                                          Integer semester, String academicYear,
                                                          AcademicStanding standing) {
        List<Student> cohort = studentRepository.findCohort(programId, yearLevel, semester,
                academicYear == null || academicYear.isBlank() ? null : academicYear);
        if (cohort.isEmpty()) {
            return List.of();
        }

        List<UUID> studentIds = cohort.stream().map(Student::getStudentId).toList();

        Map<UUID, List<CourseGrade>> gradesByStudent = new HashMap<>();
        for (CourseGrade grade : courseGradeRepository.findByStudentIds(studentIds)) {
            gradesByStudent
                    .computeIfAbsent(grade.getStudent().getStudentId(), k -> new java.util.ArrayList<>())
                    .add(grade);
        }

        Map<UUID, Double> requiredCredits = requiredCreditsByProgram();

        return cohort.stream()
                .map(student -> toSummary(student,
                        gradesByStudent.getOrDefault(student.getStudentId(), List.of()),
                        requiredCredits))
                .filter(row -> standing == null || row.standing() == standing)
                .sorted(Comparator
                        // Students needing attention lead, then by name — the
                        // list is read top-down and acted on from the top.
                        .comparingInt((StudentAcademicSummaryResponse r) -> r.standing().ordinal())
                        .reversed()
                        .thenComparing(r -> r.fullName() != null ? r.fullName() : "",
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private StudentAcademicSummaryResponse toSummary(Student student, List<CourseGrade> grades,
                                                     Map<UUID, Double> requiredCredits) {
        GpaCalculator.Gpa gpa = gpaCalculator.fromEntities(grades);
        AcademicStanding standing = AcademicStanding.of(gpa.cumulativeGpa());

        int posted = 0;
        int inProgress = 0;
        for (CourseGrade grade : grades) {
            if (grade.getStatus() == CourseGradeStatus.POSTED) {
                posted++;
            } else {
                inProgress++;
            }
        }

        Program program = student.getProgram();
        Double required = program != null ? requiredCredits.get(program.getId()) : null;

        // Graduating takes both the credits and the standing: a student can
        // finish every course and still be below the progression minimum.
        boolean eligible = required != null
                && gpa.creditsEarned() != null
                && gpa.creditsEarned() >= required
                && standing != AcademicStanding.PROBATION
                && standing != AcademicStanding.NO_RECORD;

        return new StudentAcademicSummaryResponse(
                student.getStudentId(),
                student.getStudentCode(),
                student.getUser() != null ? student.getUser().getFullName() : null,
                avatarUrlOf(student),

                program != null ? program.getId() : null,
                program != null ? program.getProgramName() : null,
                student.getYearLevel(),
                student.getSemester(),
                student.getAcademicYear(),
                student.getStatus(),

                gpa.creditsEarned(),
                gpa.creditsAttempted(),
                required,

                gpa.cumulativeGpa(),
                gpa.currentGpa(),

                standing,
                standing.getLabel(),

                posted,
                inProgress,
                eligible);
    }

    /**
     * Total credits each programme's curriculum requires.
     *
     * <p>Summed here rather than in SQL because a subject offered in two terms
     * is one requirement, while two different subjects worth three credits are
     * two — a distinction {@code sum(distinct credit)} cannot make.
     */
    private Map<UUID, Double> requiredCreditsByProgram() {
        Map<UUID, Double> totals = new HashMap<>();
        Map<UUID, Set<UUID>> counted = new HashMap<>();

        for (CurriculumRepository.ProgramSubjectCredit row : curriculumRepository.findCurriculumCredits()) {
            if (row.getCredit() == null) {
                continue;
            }
            if (!counted.computeIfAbsent(row.getProgramId(), k -> new HashSet<>())
                    .add(row.getSubjectId())) {
                continue;
            }
            totals.merge(row.getProgramId(), row.getCredit(), Double::sum);
        }
        return totals;
    }

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
}
