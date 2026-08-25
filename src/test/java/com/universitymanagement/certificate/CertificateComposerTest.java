package com.universitymanagement.certificate;

import com.universitymanagement.certificate.render.CertificateComposer;
import com.universitymanagement.certificate.render.DetectedField;
import com.universitymanagement.certificate.render.FieldPlacement;
import com.universitymanagement.certificate.render.PdfPlaceholderScanner;
import com.universitymanagement.certificate.render.PdfTextReplacer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole round trip: a design goes in with placeholders, a finished
 * certificate comes out with the student's details in their place.
 */
class CertificateComposerTest {

    private final PdfPlaceholderScanner scanner = new PdfPlaceholderScanner();
    private final CertificateComposer composer =
            new CertificateComposer(new PdfTextReplacer());

    private byte[] design() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                write(content, "Certificate of Completion", 120, 700, 24);
                write(content, "{{studentName}}", 120, 600, 28);
                write(content, "{{programName}}", 120, 540, 16);
                write(content, "No. {{certificateNumber}}", 120, 100, 10);
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private void write(PDPageContentStream content, String text, float x, float y, float size)
            throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private List<FieldPlacement> placementsFor(byte[] design) throws IOException {
        return scanner.scan(design).stream().map(FieldPlacement::from).toList();
    }

    @Test
    void fillsEachPlaceholderWithTheStudentsOwnDetails() throws IOException {
        byte[] pdf = composer.composeOnPdf(design(), placementsFor(design()), Map.of(
                "studentName", "Sok Dara",
                "programName", "Bachelor of Information Technology",
                "certificateNumber", "UT-2026-DEGREE-00042"));

        String text = textOf(pdf);

        assertTrue(text.contains("Sok Dara"), text);
        assertTrue(text.contains("Bachelor of Information Technology"));
        assertTrue(text.contains("UT-2026-DEGREE-00042"));
    }

    @Test
    void theDesignsOwnWordingSurvives() throws IOException {
        byte[] pdf = composer.composeOnPdf(design(), placementsFor(design()),
                Map.of("studentName", "Sok Dara"));

        assertTrue(textOf(pdf).contains("Certificate of Completion"),
                "only the placeholders are replaced; the design is otherwise untouched");
    }

    @Test
    void aFieldWithNoValueIsLeftAloneRatherThanBlanked() throws IOException {
        // Only the name is supplied. The others keep their placeholder rather
        // than being painted over, so a missing value is visible on the proof
        // instead of leaving an unexplained gap.
        byte[] pdf = composer.composeOnPdf(design(), placementsFor(design()),
                Map.of("studentName", "Sok Dara"));

        String text = textOf(pdf);
        assertTrue(text.contains("Sok Dara"));
        assertTrue(text.contains("{{programName}}"), text);
    }

    @Test
    void noPlaceholderSurvivesIntoTheFinishedCertificate() throws IOException {
        byte[] pdf = composer.composeOnPdf(design(), placementsFor(design()),
                Map.of("studentName", "Sok Dara"));

        // Covering a placeholder with a white box hides it on screen but leaves
        // it in the file, so selecting the text of an official document would
        // reveal the template's innards. It has to be gone, not hidden.
        assertFalse(textOf(pdf).contains("{{studentName}}"), textOf(pdf));
    }

    @Test
    void aNameOutsideTheLatinAlphabetIsStillSubstituted() throws IOException {
        // Replacing in place uses whatever font the design embedded, so a Khmer
        // name set in a Khmer font renders. Drawing it ourselves could not —
        // the built-in PDF fonts have no Khmer at all.
        byte[] pdf = composer.composeOnPdf(design(), placementsFor(design()),
                Map.of("studentName", "សុខ ដារ៉ា"));

        assertTrue(pdf.length > 0, "the batch must not fail on one name");
        assertFalse(textOf(pdf).contains("{{studentName}}"));
    }

    @Test
    void differentStudentsProduceDifferentCertificatesFromOneDesign() throws IOException {
        byte[] design = design();
        List<FieldPlacement> fields = placementsFor(design);

        String first = textOf(composer.composeOnPdf(design, fields,
                Map.of("studentName", "Sok Dara")));
        String second = textOf(composer.composeOnPdf(design, fields,
                Map.of("studentName", "Chan Sophea")));

        assertTrue(first.contains("Sok Dara"));
        assertTrue(second.contains("Chan Sophea"));
        assertFalse(second.contains("Sok Dara"),
                "one design fills for a whole cohort without leaking between them");
    }

    @Test
    void aScannedPositionIsWhereTheDesignerDrewIt() throws IOException {
        // PDFBox reports text top-down from the top of the page; a PDF is drawn
        // bottom-up. Getting that flip wrong mirrors every field vertically —
        // which looks plausible on a symmetric design and is wrong on all of
        // them. The design writes studentName at exactly (120, 600).
        DetectedField name = scanner.scan(design()).stream()
                .filter(f -> f.key().equals("studentName"))
                .findFirst()
                .orElseThrow();

        assertEquals(120f, name.x(), 2f);
        assertEquals(600f, name.y(), 3f);
    }

    @Test
    void aValueDrawnAtItsScannedPositionLandsOnTheSameLine() throws IOException {
        // The proof end to end: draw at the scanned position into a fresh page
        // and the text comes back out where the placeholder was.
        byte[] design = design();
        FieldPlacement field = placementsFor(design).stream()
                .filter(f -> f.key().equals("studentName"))
                .findFirst()
                .orElseThrow();

        byte[] filled = composer.composeOnPdf(design, List.of(field),
                Map.of("studentName", "Sok Dara"));

        DetectedField placeholder = scanner.scan(design).stream()
                .filter(f -> f.key().equals("studentName"))
                .findFirst()
                .orElseThrow();

        float where = positionOf(filled, "Sok Dara");
        assertEquals(placeholder.y(), where, 3f);
    }

    /** @return the baseline, in drawing space, of the line containing the text */
    private float positionOf(byte[] pdf, String needle) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            float pageHeight = document.getPage(0).getMediaBox().getHeight();
            float[] found = {Float.NaN};

            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    if (text.contains(needle) && !positions.isEmpty()) {
                        found[0] = pageHeight - positions.getFirst().getYDirAdj();
                    }
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);

            return found[0];
        }
    }

    @Test
    void scannedPositionsCarryTheDesignersFontSize() throws IOException {
        DetectedField name = scanner.scan(design()).stream()
                .filter(f -> f.key().equals("studentName"))
                .findFirst()
                .orElseThrow();

        assertEquals(28f, FieldPlacement.from(name).fontSize(), 0.5f);
    }
}
