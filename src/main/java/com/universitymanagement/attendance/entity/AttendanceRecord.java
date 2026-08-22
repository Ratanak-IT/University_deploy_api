package com.universitymanagement.attendance.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One student's mark for one session.
 *
 * <p>No row means the student has not been marked for that session yet, which
 * is deliberately different from being marked absent.
 */
@Entity
@Table(
        name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_record_session_student",
                columnNames = {"session_id", "student_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    /**
     * How late, when known. A policy can turn "more than N minutes" into a
     * full absence, which a bare LATE flag could never express.
     */
    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(columnDefinition = "TEXT")
    private String remark;

    /** Supporting note for an excused absence — medical certificate, letter. */
    @Column(name = "excuse_reference", length = 300)
    private String excuseReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_teacher_id")
    private Teacher recordedByTeacher;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
