package com.universitymanagement.identity.auth.dto.response;

import com.universitymanagement.admin.dto.GenderOption;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder

public class UserProfileResponse {

    private UUID id;
    private String keycloakId;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private GenderOption gender;
    private Boolean isActive;
}
