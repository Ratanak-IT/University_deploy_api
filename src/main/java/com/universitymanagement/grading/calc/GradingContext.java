package com.universitymanagement.grading.calc;

import com.universitymanagement.grading.entity.Assessment;
import com.universitymanagement.grading.entity.AssessmentScore;
import com.universitymanagement.grading.entity.GradeComponent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything needed to grade every student of a set of classrooms, loaded up
 * front in a handful of queries.
 *
 * <p>The calculator takes this and touches no repository, which is what lets a
 * 200-student record sheet render without issuing a query per student.
 */
public record GradingContext(
        Map<UUID, List<GradeComponent>> componentsByClassroom,
        Map<UUID, List<Assessment>> assessmentsByComponent,
        /** assessmentId -> studentId -> score */
        Map<UUID, Map<UUID, AssessmentScore>> scores,
        /** classroomId -> studentId -> tally */
        Map<UUID, Map<UUID, AttendanceTally>> attendance
) {

    public List<GradeComponent> componentsOf(UUID classroomId) {
        return componentsByClassroom.getOrDefault(classroomId, List.of());
    }

    public List<Assessment> assessmentsOf(UUID componentId) {
        return assessmentsByComponent.getOrDefault(componentId, List.of());
    }

    public AssessmentScore scoreOf(UUID assessmentId, UUID studentId) {
        return scores.getOrDefault(assessmentId, Map.of()).get(studentId);
    }

    public AttendanceTally attendanceOf(UUID classroomId, UUID studentId) {
        return attendance.getOrDefault(classroomId, Map.of())
                .getOrDefault(studentId, AttendanceTally.empty());
    }
}
