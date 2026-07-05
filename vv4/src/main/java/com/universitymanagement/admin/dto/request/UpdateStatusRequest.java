package com.universitymanagement.admin.dto.request;

import com.universitymanagement.identity.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull
        AccountStatus status
) {
}
