package com.universitymanagement.grading.service;

import com.universitymanagement.attendance.calc.AttendanceMath;
import com.universitymanagement.attendance.entity.AttendancePolicy;
import com.universitymanagement.attendance.entity.AttendanceRecord;
import com.universitymanagement.attendance.repository.AttendancePolicyRepository;
import com.universitymanagement.attendance.repository.AttendanceRecordRepository;
import com.universitymanagement.grading.calc.AttendanceTally;
import com.universitymanagement.grading.calc.GradingContext;
import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.ComponentSource;
import com.universitymanagement.grading.entity.GradeComponent;
import com.universitymanagement.grading.repository.AssessmentScoreRepository;
import com.universitymanagement.grading.repository.GradeComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads the grading inputs for a set of classrooms in a fixed number of queries,
 * regardless of how many students or assessments are involved.
 */
@Service
@RequiredArgsConstructor
public class GradingContextLoader {

    private final GradeComponentRepository componentRepository;
    private final AssessmentScoreRepository scoreRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceMath attendanceMath;

    @Transactional(readOnly = true)
    public GradingContext load(List<UUID> classroomIds) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return new GradingContext(Map.of(), Map.of(), Map.of(), Map.of());
        }

        List<GradeComponent> components =
                componentRepository.findWithAssessmentsByClassroomIds(classroomIds);

        Map<UUID, List<GradeComponent>> componentsByClassroom = new HashMap<>();
        Map<UUID, List<Assessment>> assessmentsByComponent = new HashMap<>();

        for (GradeComponent component : components) {
            componentsByClassroom
                    .computeIfAbsent(component.getClassroom().getClassroomId(), k -> new ArrayList<>())
                    .add(component);

            if (component.getSource() != ComponentSource.ATTENDANCE) {
                List<Assessment> live = component.getAssessments().stream()
                        .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                        .sorted(Comparator.comparing(Assessment::getPosition,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
                assessmentsByComponent.put(component.getComponentId(), live);
            }
        }

        componentsByClassroom.values().forEach(list -> list.sort(
                Comparator.comparing(GradeComponent::getPosition,
                        Comparator.nullsLast(Comparator.naturalOrder()))));

        Map<UUID, Map<UUID, AssessmentScore>> scores = new HashMap<>();
        for (AssessmentScore score : scoreRepository.findByClassrooms(classroomIds)) {
            scores.computeIfAbsent(score.getAssessment().getAssessmentId(), k -> new HashMap<>())
                    .put(score.getStudent().getStudentId(), score);
        }

        return new GradingContext(componentsByClassroom, assessmentsByComponent, scores,
                loadAttendance(classroomIds));
    }

    public GradingContext load(UUID classroomId) {
        return load(List.of(classroomId));
    }

    /**
     * Attendance credit per student, under each classroom's own policy.
     *
     * <p>Only held sessions are read, so a cancelled class or one that was
     * never marked cannot pull a student's grade down.
     */
    private Map<UUID, Map<UUID, AttendanceTally>> loadAttendance(List<UUID> classroomIds) {
        Map<UUID, AttendancePolicy> policies = new HashMap<>();
        for (AttendancePolicy policy : attendancePolicyRepository
                .findByClassroom_ClassroomIdIn(classroomIds)) {
            policies.put(policy.getClassroom().getClassroomId(), policy);
        }

        Map<UUID, Map<UUID, List<AttendanceRecord>>> grouped = new HashMap<>();
        for (AttendanceRecord record : attendanceRecordRepository.findHeldByClassrooms(classroomIds)) {
            if (record.getSession() == null || record.getStudent() == null) {
                continue;
            }
            grouped
                    .computeIfAbsent(record.getSession().getClassroom().getClassroomId(),
                            k -> new HashMap<>())
                    .computeIfAbsent(record.getStudent().getStudentId(), k -> new ArrayList<>())
                    .add(record);
        }

        Map<UUID, Map<UUID, AttendanceTally>> byClassroom = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, List<AttendanceRecord>>> entry : grouped.entrySet()) {
            AttendancePolicy policy = policies.get(entry.getKey());
            if (policy == null) {
                policy = new AttendancePolicy();
            }

            Map<UUID, AttendanceTally> byStudent = new HashMap<>();
            for (Map.Entry<UUID, List<AttendanceRecord>> student : entry.getValue().entrySet()) {
                AttendanceMath.Tally tally =
                        attendanceMath.tally(student.getValue(), student.getValue().size(), policy);
                byStudent.put(student.getKey(),
                        new AttendanceTally(tally.earned(), tally.counted()));
            }
            byClassroom.put(entry.getKey(), byStudent);
        }
        return byClassroom;
    }
}
