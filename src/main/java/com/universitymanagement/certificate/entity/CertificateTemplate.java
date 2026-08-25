package com.universitymanagement.certificate.entity;

import com.universitymanagement.auditing.BasedEntity;
import com.universitymanagement.program.entity.Program;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The blank a certificate is printed from — uploaded once, used for everyone.
 *
 * <p>The body is HTML holding placeholders like <code>{{studentName}}</code>,
 * which issuing substitutes. Keeping the design as an uploadable document is
 * what lets the registrar restyle a certificate without a developer and a
 * deploy.
 */
@Entity
@Table(name = "certificate_templates")
@Getter
@Setter
@NoArgsConstructor
public class CertificateTemplate extends BasedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 40)
    private CertificateType certificateType;

    @Column(nullable = false, length = 150)
    private String name;

    /**
     * How this template produces a certificate.
     *
     * <p>HTML is written in the editor; OVERLAY fills an uploaded PDF or image
     * design. Both end at the same place — values substituted into a layout —
     * but only one of them survives being opened in a different word processor.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "render_mode", nullable = false, length = 20)
    private RenderMode renderMode = RenderMode.HTML;

    /** HTML with {{placeholders}}. Rendered, never executed. Used by HTML mode. */
    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    /* ---- OVERLAY mode: the uploaded design ---- */

    /** MinIO object name of the uploaded PDF or image. */
    @Column(name = "asset_object_name")
    private String assetObjectName;

    @Column(name = "asset_original_name")
    private String assetOriginalName;

    /** "PDF", "IMAGE" — decides which composer path runs. */
    @Column(name = "asset_kind", length = 10)
    private String assetKind;

    /** Page size in PDF points, so the editor can scale its overlay. */
    @Column(name = "asset_width")
    private Float assetWidth;

    @Column(name = "asset_height")
    private Float assetHeight;

    /**
     * Field positions as JSON.
     *
     * <p>Stored as a document rather than a child table because it is only ever
     * read and written whole, with the template — splitting it into rows would
     * buy joins nobody needs.
     */
    @Column(name = "fields_json", columnDefinition = "TEXT")
    private String fieldsJson;

    /**
     * Null means the template serves every programme. A programme-specific one
     * wins over the general fallback when both exist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(length = 300)
    private String description;
}
