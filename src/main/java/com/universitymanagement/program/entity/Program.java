package com.universitymanagement.program.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.curriculum.entity.Curriculum;
import com.universitymanagement.department.entity.Department;
import com.universitymanagement.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class Program extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 150)
    private String programName;
    @Column(length = 50)
    private String degreeLevel;
    private Integer durationYears;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "program")
    private List<Student> students;

    @OneToMany(mappedBy = "program")
    private List<Curriculum> curriculum;

}
