package com.universitymanagement.grading.dto.request;

import com.universitymanagement.grading.entity.ComponentSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Replaces a classroom's whole grading policy in one call, because the weights
 * are only valid as a set — they have to total 100.
 */
public record SaveGradeSchemeRequest(
        @NotEmpty(message = "at least one grade component is required")
        List<@Valid ComponentItem> components
) {
    public record ComponentItem(
            /** Null to create; an existing id to keep the component's scores. */
            UUID componentId,

            @NotBlank(message = "component name is required")
            @Size(max = 100, message = "component name must be at most 100 characters")
            String name,

            @NotNull(message = "component source is required")
            ComponentSource source,

            @NotNull(message = "weightPercent is required")
            @DecimalMin(value = "0.0", message = "weightPercent cannot be negative")
            Double weightPercent,

            Integer position
    ) {
    }
}
