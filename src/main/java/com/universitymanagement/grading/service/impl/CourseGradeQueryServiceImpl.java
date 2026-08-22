package com.universitymanagement.grading.service.impl;

import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.grading.calc.ComponentResult;
import com.universitymanagement.grading.calc.CourseResult;
import com.universitymanagement.grading.calc.GradeCalculator;
import com.universitymanagement.grading.calc.GradingContext;
import com.universitymanagement.grading.dto.response.CourseGradeResponse;
import com.universitymanagement.grading.dto.response.GradebookResponse;
import com.universitymanagement.grading.entity.CourseGrade;
import com.universitymanagement.grading.entity.CourseGradeStatus;
import com.universitymanagement.grading.entity.LetterGrade;
import com.universitymanagement.grading.repository.CourseGradeRepository;
import com.universitymanagement.grading.service.CourseGradeQueryService;
import com.universitymanagement.grading.service.GradingContextLoader;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.subject.entity.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseGradeQueryServiceImpl implements CourseGradeQueryService {

    private final GradingContextLoader contextLoader;
    private final GradeCalculator calculator;
    private final CourseGradeRepository courseGradeRepository;
    private final ClassroomStudentRepository classroomStudentRepository;

    @Override
    public List<CourseGradeResponse> gradesFor(UUID studentId, List<Classroom> classrooms) {
        if (classrooms.isEmpty()) {
            return List.of();
        }

        List<UUID> classroomIds = classrooms.stream().map(Classroom::getClassroomId).toList();
        GradingContext context = contextLoader.load(classroomIds);

        Map<UUID, CourseGrade> stored = new HashMap<>();
        for (CourseGrade grade : courseGradeRepository.findByStudentWithCourse(studentId)) {
            stored.put(grade.getClassroom().getClassroomId(), grade);
        }

        List<CourseGradeResponse> responses = new ArrayList<>();
        for (Classroom classroom : classrooms) {
            responses.add(toResponse(
                    classroom,
                    null,
                    studentId,
                    calculator.calculate(classroom.getClassroomId(), studentId, context),
                    stored.get(classroom.getClassroomId())));
        }
        return responses;
    }

    @Override
    public Map<UUID, List<CourseGradeResponse>> gradesFor(List<Classroom> classrooms) {
        if (classrooms.isEmpty()) {
            return Map.of();
        }

        List<UUID> classroomIds = classrooms.stream().map(Classroom::getClassroomId).toList();
        GradingContext context = contextLoader.load(classroomIds);

        Map<UUID, List<Student>> rosterByClassroom = new HashMap<>();
        for (ClassroomStudent link : classroomStudentRepository
                .findByClassroom_ClassroomIdIn(classroomIds)) {
            if (link.getStudent() == null || link.getClassroom() == null) {
                continue;
            }
            rosterByClassroom
                    .computeIfAbsent(link.getClassroom().getClassroomId(), k -> new ArrayList<>())
                    .add(link.getStudent());
        }

        Map<UUID, Map<UUID, CourseGrade>> stored = new HashMap<>();
        for (CourseGrade grade : courseGradeRepository.findByClassroomsWithCourse(classroomIds)) {
            stored.computeIfAbsent(grade.getClassroom().getClassroomId(), k -> new HashMap<>())
                    .put(grade.getStudent().getStudentId(), grade);
        }

        Map<UUID, List<CourseGradeResponse>> byStudent = new LinkedHashMap<>();
        for (Classroom classroom : classrooms) {
            UUID classroomId = classroom.getClassroomId();
            Map<UUID, CourseGrade> storedHere = stored.getOrDefault(classroomId, Map.of());

            for (Student student : rosterByClassroom.getOrDefault(classroomId, List.of())) {
                CourseResult result =
                        calculator.calculate(classroomId, student.getStudentId(), context);

                byStudent.computeIfAbsent(student.getStudentId(), k -> new ArrayList<>())
                        .add(toResponse(classroom, student, student.getStudentId(), result,
                                storedHere.get(student.getStudentId())));
            }
        }
        return byStudent;
    }

    private CourseGradeResponse toResponse(Classroom classroom, Student student, UUID studentId,
                                           CourseResult result, CourseGrade stored) {
        // A posted grade is a matter of record; recalculating it would let a late
        // score edit rewrite a transcript that has already been issued.
        boolean posted = stored != null && stored.getStatus() == CourseGradeStatus.POSTED;

        Double percent = posted ? stored.getScorePercent() : result.scorePercent();
        LetterGrade letter = posted ? stored.getLetterGrade() : result.letterGrade();
        Double point = posted ? stored.getGradePoint() : result.gradePoint();
        Double completeness = posted ? stored.getCompletenessPercent() : result.completenessPercent();

        Subject subject = classroom.getSubject();
        Student subjectStudent = student != null
                ? student
                : (stored != null ? stored.getStudent() : null);

        return new CourseGradeResponse(
                stored != null ? stored.getCourseGradeId() : null,
                studentId,
                subjectStudent != null ? subjectStudent.getStudentCode() : null,
                subjectStudent != null && subjectStudent.getUser() != null
                        ? subjectStudent.getUser().getFullName() : null,

                classroom.getClassroomId(),
                classroom.getClassName(),
                subject != null ? subject.getSubjectId() : null,
                subject != null ? subject.getSubjectCode() : null,
                subject != null ? subject.getSubjectName() : null,
                subject != null ? subject.getCredit() : null,
                classroom.getAcademicYear(),
                classroom.getSemester(),

                percent,
                letter != null ? letter.getDisplay() : null,
                point,
                stored != null ? stored.getCreditsEarned() : null,
                completeness,
                stored != null ? stored.getStatus() : CourseGradeStatus.IN_PROGRESS,
                stored == null || Boolean.TRUE.equals(stored.getCountsInGpa()),
                stored != null ? stored.getPostedAt() : null,
                stored != null ? stored.getRemark() : null,

                result.components().stream().map(this::toBreakdown).filter(Objects::nonNull).toList());
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
