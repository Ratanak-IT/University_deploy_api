package com.universitymanagement.identity.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @NotBlank
    @Size(max = 150)
    private String fullName;

    @Email
    private String email;

    @Pattern(
            regexp = "^\\+?[0-9]{8,15}$"
    )
    private String phone;
}
