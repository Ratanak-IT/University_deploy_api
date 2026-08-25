package com.universitymanagement.certificate.render;

/**
 * Where one value is printed on the design, and how.
 *
 * <p>Produced either by {@link PdfPlaceholderScanner} reading a PDF, or by the
 * admin dragging a box over an uploaded image. Both paths end here, which is
 * why one composer can serve both.
 */
public record FieldPlacement(
        String key,
        int page,
        float x,
        float y,
        float width,
        float fontSize,
        /** LEFT, CENTER or RIGHT within {@link #width}. */
        String align,
        /** Hex, e.g. #0b1c30. */
        String color,
        boolean bold
) {
    public static FieldPlacement from(DetectedField detected) {
        return new FieldPlacement(
                detected.key(),
                detected.page(),
                detected.x(),
                detected.y(),
                detected.width(),
                detected.fontSize(),
                // Detected placeholders sit exactly where their text began, so
                // filling from the same point keeps the design's own alignment.
                "LEFT",
                "#000000",
                detected.fontName() != null
                        && detected.fontName().toLowerCase().contains("bold"));
    }
}
