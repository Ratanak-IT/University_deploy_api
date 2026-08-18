package com.universitymanagement.identity.entity;

import com.universitymanagement.admin.dto.GenderOption;
import com.universitymanagement.auditing.BasedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "name_khmer")
    private String nameKhmer;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GenderOption gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "id_card_number", length = 50)
    private String idCardNumber;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Column(name = "current_address", columnDefinition = "TEXT")
    private String currentAddress;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "avatar_object_name")
    private String avatarObjectName;

    @Column(name = "account_status")
    private String accountStatus;

    private String fatherContact;
    private String motherContact;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;


    public String resolvedFirstName() {
        if (firstName != null && !firstName.isBlank()) return firstName;
        if (fullName == null || fullName.isBlank()) return null;
        return fullName.trim().split("\\s+")[0];
    }

    public String resolvedLastName() {
        if (lastName != null && !lastName.isBlank()) return lastName;
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    public void syncFullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String joined = (first + " " + last).trim();
        if (!joined.isBlank()) {
            this.fullName = joined;
        }
    }
}