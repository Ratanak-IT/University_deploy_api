package com.universitymanagement.certificate.render;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds <code>{{placeholders}}</code> in an uploaded PDF and reports where each
 * one sits.
 *
 * <p>This is what makes "upload your own design" work without OCR. A PDF is not
 * a picture of text — it stores drawing instructions that already carry each
 * character's position, font and size. Reading those is exact; recognising
 * shapes in a JPEG is a guess, and a guess that is wrong 3% of the time still
 * puts a stranger's name on somebody's degree.
 *
 * <p>It deliberately only finds placeholders the designer typed. It does not
 * try to infer that a blank line "looks like" a name field — that inference is
 * the unreliable part, and skipping it is why this is dependable.
 */
@Component
public class PdfPlaceholderScanner {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    public List<DetectedField> scan(byte[] pdfBytes) throws IOException {
        List<DetectedField> found = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                found.addAll(scanPage(document, page));
            }
        }
        return found;
    }

    /** Page size in points, so the editor can lay its overlay out to scale. */
    public float[] pageSize(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            var box = document.getPage(0).getMediaBox();
            return new float[]{box.getWidth(), box.getHeight()};
        }
    }

    private List<DetectedField> scanPage(PDDocument document, int page) throws IOException {
        List<DetectedField> found = new ArrayList<>();

        // PDFBox reports text positions top-down from the top of the page, but
        // a PDF is drawn bottom-up. Everything downstream — the composer, the
        // editor's boxes — works in the drawing space, so the flip happens once
        // here rather than being repeated, and mis-remembered, at each use.
        float pageHeight = document.getPage(page - 1).getMediaBox().getHeight();

        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                Matcher matcher = PLACEHOLDER.matcher(text);

                while (matcher.find()) {
                    // The match is over the joined string, so its character
                    // range indexes straight into the position list — that is
                    // how a placeholder split across several draw operations
                    // still resolves to one box.
                    int from = matcher.start();
                    int to = Math.min(matcher.end(), positions.size());
                    if (from >= positions.size()) {
                        continue;
                    }

                    List<TextPosition> span = positions.subList(from, to);
                    if (span.isEmpty()) {
                        continue;
                    }

                    TextPosition first = span.getFirst();
                    float minX = Float.MAX_VALUE;
                    float maxX = -Float.MAX_VALUE;
                    float top = Float.MAX_VALUE;
                    float bottom = -Float.MAX_VALUE;

                    for (TextPosition position : span) {
                        minX = Math.min(minX, position.getXDirAdj());
                        maxX = Math.max(maxX, position.getXDirAdj() + position.getWidthDirAdj());
                        top = Math.min(top, position.getYDirAdj() - position.getHeightDir());
                        bottom = Math.max(bottom, position.getYDirAdj());
                    }

                    found.add(new DetectedField(
                            matcher.group(1),
                            page,
                            minX,
                            // The bottom edge in top-down space is the baseline
                            // in drawing space, which is where text is written
                            // from — so a filled value lands on the same line
                            // the placeholder sat on.
                            pageHeight - bottom,
                            maxX - minX,
                            bottom - top,
                            fontNameOf(first),
                            first.getFontSizeInPt()));
                }
            }
        };

        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(document);

        return found;
    }

    /** Strips the subset prefix PDFs add, e.g. "ABCDEF+Georgia-Bold". */
    private String fontNameOf(TextPosition position) {
        try {
            String name = position.getFont().getName();
            if (name == null) {
                return "Helvetica";
            }
            int plus = name.indexOf('+');
            return plus > 0 && plus < name.length() - 1 ? name.substring(plus + 1) : name;
        } catch (Exception e) {
            return "Helvetica";
        }
    }
}
