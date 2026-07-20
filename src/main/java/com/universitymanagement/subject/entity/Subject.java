package com.universitymanagement.subject.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.curriculum.entity.Curriculum;
import com.universitymanagement.department.entity.Department;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID subjectId;
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Subject code cannot be blank")
    private String subjectCode;
    @Column(nullable = false)
    @NotBlank(message = "Subject code cannot be blank")
    private String subjectName;
    private Boolean isDeleted;
    @Column(nullable = false)
    @NotNull(message = "Subject credit cannot be null")
    private Double credit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "subject")
    private List<Curriculum> curriculum;
}
