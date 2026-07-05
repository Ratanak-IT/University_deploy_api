package com.universitymanagement.identity.entity;


import com.universitymanagement.auditing.BasedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "permission_name", nullable = false, unique = true)
    private String name;

    private String description;
}
