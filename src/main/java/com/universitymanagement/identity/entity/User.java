package com.universitymanagement.identity.entity;

import com.universitymanagement.admin.dto.GenderOption;
import com.universitymanagement.auditing.BasedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "IdentityUser")
@Table(name = "users")

public class User extends BasedEntity {
    @Id
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "profile_image_file_id")
    private Long profileImageFileId;

    @Column(name = "account_status")
    private String accountStatus;

    GenderOption gender;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

}
