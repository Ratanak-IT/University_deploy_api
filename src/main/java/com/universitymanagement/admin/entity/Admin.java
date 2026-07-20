package com.universitymanagement.admin.entity;


import com.universitymanagement.auditing.BasedEntity;

import com.universitymanagement.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "admins")
public class Admin extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID adminId;

    // IMPORTANT: each admin is ONE user
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true)
    private User user;

    @Column(name = "admin_code", unique = true, nullable = false)
    private String adminCode;

    private String position;
    private String department;

}
