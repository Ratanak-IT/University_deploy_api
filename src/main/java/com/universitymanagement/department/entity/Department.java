package com.universitymanagement.department.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.program.entity.Program;
import com.universitymanagement.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID departmentId;
    @Column(nullable = false, length = 100)
    private String departmentName;
    private Boolean isDeleted;
    private String departmentCode;

    /**
     * Free text the registrar writes about the department.
     *
     * <p>Nullable on purpose. Adding a NOT NULL column to a table that already
     * holds departments would fail the schema update outright, the way
     * quiz_attempts.focus_loss_count did — and a department that has never had
     * a description written for it genuinely has none, which is what null says.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Subject> subjects = new ArrayList<>();

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Program> programs = new ArrayList<>();
}
