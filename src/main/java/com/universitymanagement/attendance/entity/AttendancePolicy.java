package com.universitymanagement.attendance.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The attendance rules for one classroom.
 *
 * <p>These were previously constants inside the grade calculator — a lateness
 * worth half a session, and no eligibility rule at all. A rule that decides
 * whether a student may sit their final exam has to be visible and editable by
 * the people who set it, not buried in Java.
 */
@Entity
@Table(
        name = "attendance_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_policy_classroom",
                columnNames = "classroom_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AttendancePolicy extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    /** Credit a late arrival earns, 0–1. 0.5 means half a session. */
    @Column(name = "late_credit", nullable = false)
    private Double lateCredit = 0.5;

    /** Past this many minutes, a late arrival is recorded as an absence instead. */
    @Column(name = "late_becomes_absent_after_minutes")
    private Integer lateBecomesAbsentAfterMinutes = 30;

    /** Attendance below this bars the student from the final exam. Null disables the rule. */
    @Column(name = "min_percent_to_sit_exam")
    private Double minPercentToSitExam = 80.0;

    /** Excused absences leave the denominator rather than scoring zero. */
    @Column(name = "excused_absences_ignored", nullable = false)
    private Boolean excusedAbsencesIgnored = true;

    /** The institution's default, used until a classroom sets its own. */
    public static AttendancePolicy defaultFor(Classroom classroom) {
        AttendancePolicy policy = new AttendancePolicy();
        policy.setClassroom(classroom);
        return policy;
    }
}
