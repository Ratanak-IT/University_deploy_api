package com.universitymanagement.teacher.entity;


import com.universitymanagement.department.entity.Department;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "teachers")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID teacherId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "teacher_code", unique = true)
    private String teacherCode;

    private String specialization;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_departments",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    private String position;

    private LocalDate hireDate;


    /**
     * Withdrawn from the university, but not erased.
     *
     * <p>A teacher's record is the anchor for attendance, marks, submissions and
     * certificates. Deleting the row means deleting all of that with it — the
     * previous implementation did exactly that, and a graduate whose transcript
     * had been destroyed could never be issued one again.
     *
     * <p>So removal hides the record and disables the sign-in account instead.
     * Nullable because the column is added to a table that already holds rows:
     * a NOT NULL column with no default fails the schema update outright.
     */
    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    private String employmentStatus;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Set<Subject> subjects = new HashSet<>();
}
