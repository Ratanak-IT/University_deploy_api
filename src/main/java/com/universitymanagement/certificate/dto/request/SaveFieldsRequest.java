package com.universitymanagement.certificate.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Where each value prints on an uploaded design.
 *
 * <p>Comes either from the scanner (a PDF that carried placeholders) or from
 * the admin dragging boxes over an image. The shape is the same either way, so
 * one editor and one composer serve both.
 */
public record SaveFieldsRequest(
        @NotNull(message = "fields are required")
        List<@Valid Field> fields
) {
    public record Field(
            @NotBlank(message = "key is required")
            String key,

            Integer page,

            @NotNull(message = "x is required")
            Float x,

            @NotNull(message = "y is required")
            Float y,

            Float width,
            Float fontSize,

            /** LEFT, CENTER or RIGHT. */
            String align,

            /** Hex, e.g. #0b1c30. */
            String color,

            Boolean bold
    ) {
    }
}
