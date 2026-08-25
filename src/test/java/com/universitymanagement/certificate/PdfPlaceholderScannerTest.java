package com.universitymanagement.certificate;

import com.universitymanagement.certificate.render.DetectedField;
import com.universitymanagement.certificate.render.PdfPlaceholderScanner;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The feature rests on one claim: a PDF already records where its text sits, so
 * placeholders can be located exactly rather than recognised from pixels. These
 * build real PDFs and check that claim holds.
 */
class PdfPlaceholderScannerTest {

    private final PdfPlaceholderScanner scanner = new PdfPlaceholderScanner();

    /** Builds a one-page PDF with each line drawn at a known position. */
    private byte[] pdfWith(List<Line> lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                for (Line line : lines) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), line.size());
                    content.newLineAtOffset(line.x(), line.y());
                    content.showText(line.text());
                    content.endText();
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private record Line(String text, float x, float y, float size) {
    }

    private Map<String, DetectedField> byKey(List<DetectedField> fields) {
        return fields.stream().collect(Collectors.toMap(DetectedField::key, Function.identity()));
    }

    @Test
    void findsEveryPlaceholderInTheDesign() throws IOException {
        byte[] pdf = pdfWith(List.of(
                new Line("{{studentName}}", 240, 500, 28),
                new Line("{{programName}}", 240, 440, 18),
                new Line("No. {{certificateNumber}}", 100, 90, 10)));

        List<DetectedField> found = scanner.scan(pdf);

        assertEquals(3, found.size());
        assertEquals(
                java.util.Set.of("studentName", "programName", "certificateNumber"),
                byKey(found).keySet());
    }

    @Test
    void reportsWhereEachPlaceholderSits() throws IOException {
        byte[] pdf = pdfWith(List.of(new Line("{{studentName}}", 240, 500, 28)));

        DetectedField field = scanner.scan(pdf).getFirst();

        // Drawn at x=240; PDFBox reports from the left edge of the glyph run.
        assertTrue(Math.abs(field.x() - 240f) < 2f,
                "x should be about 240 but was " + field.x());
        assertTrue(field.width() > 0, "the box needs a width to place text in");
        assertTrue(field.height() > 0);
    }

    @Test
    void carriesTheFontSizeTheDesignerChose() throws IOException {
        byte[] pdf = pdfWith(List.of(
                new Line("{{studentName}}", 100, 500, 28),
                new Line("{{studentCode}}", 100, 400, 10)));

        Map<String, DetectedField> found = byKey(scanner.scan(pdf));

        // Filling the name at 10pt because the size was not read would produce
        // a certificate that looks nothing like the design that was approved.
        assertEquals(28f, found.get("studentName").fontSize(), 0.5f);
        assertEquals(10f, found.get("studentCode").fontSize(), 0.5f);
    }

    @Test
    void toleratesSpacesInsideTheBraces() throws IOException {
        byte[] pdf = pdfWith(List.of(new Line("{{ studentName }}", 100, 500, 14)));

        assertEquals("studentName", scanner.scan(pdf).getFirst().key());
    }

    @Test
    void aDesignWithNoPlaceholdersYieldsNothingRatherThanGuessing() throws IOException {
        byte[] pdf = pdfWith(List.of(
                new Line("Certificate of Completion", 100, 500, 24),
                new Line("________________________", 100, 400, 14)));

        // The blank line is exactly the case an OCR-style guesser would claim
        // is a name field. Reporting nothing is the honest answer.
        assertTrue(scanner.scan(pdf).isEmpty());
    }

    @Test
    void readsThePageSizeSoTheEditorCanScaleItsOverlay() throws IOException {
        byte[] pdf = pdfWith(List.of(new Line("{{studentName}}", 100, 500, 14)));

        float[] size = scanner.pageSize(pdf);

        assertEquals(PDRectangle.A4.getWidth(), size[0], 1f);
        assertEquals(PDRectangle.A4.getHeight(), size[1], 1f);
    }

    @Test
    void findsTheSamePlaceholderUsedTwice() throws IOException {
        byte[] pdf = pdfWith(List.of(
                new Line("{{studentName}}", 100, 500, 20),
                new Line("{{studentName}}", 100, 200, 12)));

        assertEquals(2, scanner.scan(pdf).size(),
                "a name printed twice needs filling in both places");
    }
}
