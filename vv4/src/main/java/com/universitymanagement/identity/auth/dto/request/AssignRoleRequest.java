package com.universitymanagement.identity.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignRoleRequest  {
    @NotNull
    private UUID userId;

    @NotNull
    private UUID roleId;
}
