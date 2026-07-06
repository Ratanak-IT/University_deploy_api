package com.universitymanagement.identity.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(
            min = 8,
            message = "Password must be between 8 and 100 characters"
    )
    private String password;
}
