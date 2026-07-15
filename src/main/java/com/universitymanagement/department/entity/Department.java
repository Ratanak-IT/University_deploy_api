package com.universitymanagement.department.entity;

import com.universitymanagement.auditing.BasedEntity;
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
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Subject> subjects = new ArrayList<>();
}
