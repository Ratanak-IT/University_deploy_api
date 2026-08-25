package com.universitymanagement.certificate.render;

/**
 * A placeholder found in an uploaded design, with where it sits on the page.
 *
 * <p>Coordinates are in PDF points from the bottom-left of the page, which is
 * the space PDFBox reports and writes in. The editor converts to top-left
 * pixels for display and back again on save, so the stored value never depends
 * on the browser's zoom or the preview's size.
 */
public record DetectedField(
        String key,
        int page,
        float x,
        float y,
        float width,
        float height,
        String fontName,
        float fontSize
) {
}
