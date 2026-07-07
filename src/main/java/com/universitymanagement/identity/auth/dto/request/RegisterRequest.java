
package com.universitymanagement.identity.auth.dto.request;

import com.universitymanagement.admin.dto.GenderOption;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$")
    private String phoneNumber;

    @NotNull(message = "Gender is required")
    private GenderOption gender;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String confirmPassword;
}
