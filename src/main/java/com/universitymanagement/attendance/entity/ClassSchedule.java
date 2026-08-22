package com.universitymanagement.attendance.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A recurring slot on the timetable — "Mondays, 08:00–10:00, lecture".
 *
 * <p>Sessions are generated from these across the term's dates, so a teacher
 * opens the register for a class that is already there instead of creating one
 * by hand every week. A course meeting twice a week has two of these; one
 * meeting twice on the same day has two with different times.
 */
@Entity
@Table(
        name = "class_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_class_schedule_classroom_day_start",
                columnNames = {"classroom_id", "day_of_week", "start_time"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ClassSchedule extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType type = SessionType.LECTURE;

    @Column(length = 50)
    private String room;
}
