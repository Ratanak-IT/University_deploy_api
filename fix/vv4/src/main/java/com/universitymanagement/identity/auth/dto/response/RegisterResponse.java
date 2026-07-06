package com.universitymanagement.identity.auth.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private UUID id;

    private String keycloakId;

    private String email;

    private String username;

    private String firstName;

    private String lastName;

    private Boolean isActive;

    private String accountStatus;

    private LocalDateTime createdAt;
}