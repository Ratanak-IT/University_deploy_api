package com.universitymanagement.attendance.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "attendances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_student_class_date",
                columnNames = {"student_id", "classroom_id", "attendance_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Attendance extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(columnDefinition = "TEXT")
    private String remark;
}
