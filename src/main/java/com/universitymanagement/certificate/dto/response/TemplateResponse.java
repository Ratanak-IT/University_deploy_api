package com.universitymanagement.certificate.dto.response;

import com.universitymanagement.certificate.entity.CertificateType;
import com.universitymanagement.certificate.entity.RenderMode;
import com.universitymanagement.certificate.entity.TemplateStatus;
import com.universitymanagement.certificate.render.FieldPlacement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID templateId,
        CertificateType certificateType,
        String name,
        String bodyHtml,
        UUID programId,
        String programName,
        Integer version,
        TemplateStatus status,
        String description,
        LocalDateTime lastUpdateAt,

        /** Placeholders the template uses that the renderer does not know. */
        List<String> unknownPlaceholders,

        /** HTML body, or an uploaded design filled at the placed positions. */
        RenderMode renderMode,

        /** PDF, IMAGE or null. */
        String assetKind,
        String assetOriginalName,
        Float assetWidth,
        Float assetHeight,

        /** Where each value prints. Empty unless {@code renderMode} is OVERLAY. */
        List<FieldPlacement> fields
) {
}
