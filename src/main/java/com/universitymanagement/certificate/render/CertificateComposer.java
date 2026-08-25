package com.universitymanagement.certificate.render;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Prints a student's details onto the uploaded design and returns a PDF.
 *
 * <p>The design is used as the page itself — every border, seal and typeface
 * the registrar approved survives untouched, because nothing is redrawn. Only
 * the values are added, at the positions the scanner found or the admin set.
 */
@Component
@RequiredArgsConstructor
public class CertificateComposer {

    private final PdfTextReplacer textReplacer;

    /**
     * Fills an uploaded PDF design.
     *
     * <p>Placeholders are swapped in the page's own drawing instructions, so the
     * result carries the student's details and no trace of the template. An
     * earlier version painted a white box over each placeholder and printed
     * beside it, which looked right but left <code>{{studentName}}</code> in the
     * file for anyone who selected the text.
     *
     * <p>Anything the replacer could not reach — a placeholder the exporter
     * split across drawing operations in a way that cannot be rejoined — is
     * drawn at its scanned position instead, so no field is silently dropped.
     *
     * @param background the uploaded PDF, used as the page
     */
    public byte[] composeOnPdf(byte[] background, List<FieldPlacement> fields,
                               Map<String, String> values) throws IOException {
        try (PDDocument document = Loader.loadPDF(background);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            List<String> replaced = textReplacer.replace(document, values);

            for (FieldPlacement field : fields) {
                if (replaced.contains(field.key())) {
                    continue;
                }

                int index = Math.max(field.page() - 1, 0);
                if (index >= document.getNumberOfPages()) {
                    continue;
                }

                String value = values.get(field.key());
                if (value == null || value.isBlank()) {
                    continue;
                }

                PDPage page = document.getPage(index);
                try (PDPageContentStream content = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Only reached when the text could not be replaced in place,
                    // so the placeholder is still visible and has to be covered.
                    content.setNonStrokingColor(Color.WHITE);
                    content.addRect(field.x() - 1, field.y() - 2,
                            field.width() + 2, field.fontSize() * 1.3f);
                    content.fill();

                    draw(content, field, value);
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * @param background a JPEG or PNG design, laid down as a full-page image
     */
    public byte[] composeOnImage(byte[] background, String fileName,
                                 List<FieldPlacement> fields,
                                 Map<String, String> values) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDImageXObject image = PDImageXObject.createFromByteArray(
                    document, background, fileName);

            // The page takes the image's own proportions, so a landscape
            // certificate does not get letterboxed onto a portrait sheet.
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(image, 0, 0, image.getWidth(), image.getHeight());

                for (FieldPlacement field : fields) {
                    String value = values.get(field.key());
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    draw(content, field, value);
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private void draw(PDPageContentStream content, FieldPlacement field, String value)
            throws IOException {
        PDFont font = new PDType1Font(field.bold()
                ? Standard14Fonts.FontName.HELVETICA_BOLD
                : Standard14Fonts.FontName.HELVETICA);

        float size = field.fontSize() > 0 ? field.fontSize() : 12f;
        String text = sanitise(value);

        float textWidth;
        try {
            textWidth = font.getStringWidth(text) / 1000f * size;
        } catch (Exception e) {
            textWidth = text.length() * size * 0.5f;
        }

        float x = switch (field.align() == null ? "LEFT" : field.align()) {
            case "CENTER" -> field.x() + (field.width() - textWidth) / 2f;
            case "RIGHT" -> field.x() + field.width() - textWidth;
            default -> field.x();
        };

        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(parseColor(field.color()));
        content.newLineAtOffset(x, field.y());
        content.showText(text);
        content.endText();
    }

    /**
     * The standard PDF fonts cover WinAnsi only. A character outside it — a
     * Khmer name, a curly quote pasted from Word — throws when written, which
     * would fail the whole batch. Replacing it keeps the run going, and the
     * gap is visible rather than silent.
     */
    private String sanitise(String value) {
        StringBuilder safe = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            safe.append(c >= 32 && c <= 255 ? c : '?');
        }
        return safe.toString();
    }

    private Color parseColor(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return Color.BLACK;
        }
        try {
            return new Color(
                    Integer.parseInt(hex.substring(1, 3), 16),
                    Integer.parseInt(hex.substring(3, 5), 16),
                    Integer.parseInt(hex.substring(5, 7), 16));
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }
}
