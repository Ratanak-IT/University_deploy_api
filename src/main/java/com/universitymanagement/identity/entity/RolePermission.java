package com.universitymanagement.identity.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "role_id",
                                "permission_id"
                        }
                )
        }
)
@Getter
@Setter
public class RolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
