package com.universitymanagement.certificate.dto.response;

import com.universitymanagement.certificate.render.DetectedField;

import java.util.List;

/**
 * What was found in an uploaded design.
 *
 * <p>The registrar sees this immediately after uploading, before anything is
 * issued — which placeholders the design contains, which of them the system
 * recognises, and where each one sits.
 */
public record DesignUploadResponse(
        String assetKind,
        String originalName,
        Float width,
        Float height,

        /** Placeholders located with their positions. Empty for an image. */
        List<DetectedField> detected,

        /** Placeholder names found but not recognised — usually a typo. */
        List<String> unknown,

        /**
         * Set when the upload cannot be used as-is — a Word file, which has no
         * fixed layout — with what to do about it.
         */
        String guidance
) {
}
