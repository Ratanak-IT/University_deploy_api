package com.universitymanagement.attendance.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One meeting of a class — the thing attendance is actually taken against.
 *
 * <p>Attendance used to hang off a bare date, one row per student per day. A
 * course that meets twice on a Tuesday could therefore only record one of
 * them, and nothing distinguished "nobody was marked" from "everyone was
 * present" — so a student's attendance percentage moved depending on how
 * diligent the teacher had been, not on whether the student turned up.
 *
 * <p>Creating the session first fixes both: a session that exists but has no
 * marks is visibly unmarked, and two meetings on one day are two rows.
 */
@Entity
@Table(
        name = "class_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_class_session_classroom_date_start",
                columnNames = {"classroom_id", "session_date", "start_time"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ClassSession extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /** Part of the natural key: it is what separates a morning class from an afternoon one. */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType type = SessionType.LECTURE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    /** What was covered — useful on the register and in a student's own view. */
    @Column(length = 300)
    private String topic;

    /** Why it was called off. Required in the API when cancelling. */
    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taken_by_teacher_id")
    private Teacher takenByTeacher;

    /** When the register was last completed. Null means never marked. */
    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    /** Counts towards attendance only once it has actually been taught. */
    public boolean countsTowardsAttendance() {
        return status == SessionStatus.HELD;
    }
}
