package com.universitymanagement.certificate.render;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Swaps placeholder text inside a PDF for the student's own details.
 *
 * <p>Rewrites the page's drawing instructions rather than painting a white box
 * over the placeholder and printing next to it. Covering only hides it: the
 * words <code>{{studentName}}</code> stay in the file, so selecting the text of
 * a finished certificate — or letting any indexer read it — still shows the
 * template's innards. On an official document that is not acceptable.
 *
 * <p>Replacing in place also keeps the position, font and size for free: it is
 * the same drawing operation, with different characters.
 */
@Component
public class PdfTextReplacer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    /**
     * @param values placeholder key to the text that should replace it
     * @return the keys that were actually found and replaced
     */
    public List<String> replace(PDDocument document, Map<String, String> values)
            throws IOException {
        List<String> replaced = new ArrayList<>();

        for (PDPage page : document.getPages()) {
            PDFStreamParser parser = new PDFStreamParser(page);
            List<Object> tokens = parser.parse();
            boolean changed = false;

            for (int i = 0; i < tokens.size(); i++) {
                Object token = tokens.get(i);

                if (token instanceof COSString text) {
                    String swapped = substitute(text.getString(), values, replaced);
                    if (swapped != null) {
                        tokens.set(i, new COSString(swapped));
                        changed = true;
                    }
                } else if (token instanceof COSArray array) {
                    // TJ draws an array of strings with kerning between them, so
                    // a placeholder can be spread across several entries. Joining
                    // them, substituting, then putting the whole result in the
                    // first entry keeps the text intact — the kerning that
                    // applied to placeholder characters is not worth preserving.
                    if (substituteArray(array, values, replaced)) {
                        changed = true;
                    }
                }
            }

            if (changed) {
                PDStream updated = new PDStream(document);
                try (OutputStream out = updated.createOutputStream()) {
                    new ContentStreamWriter(out).writeTokens(tokens);
                }
                page.setContents(updated);
            }
        }

        return replaced;
    }

    /** @return the substituted string, or null when nothing matched */
    private String substitute(String raw, Map<String, String> values, List<String> replaced) {
        Matcher matcher = PLACEHOLDER.matcher(raw);
        if (!matcher.find()) {
            return null;
        }

        matcher.reset();
        StringBuilder out = new StringBuilder();
        boolean any = false;

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            if (value == null) {
                // Left in place on purpose: an unfilled field should be visible
                // on the proof rather than leaving an unexplained blank.
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
            if (!replaced.contains(key)) {
                replaced.add(key);
            }
            any = true;
        }
        matcher.appendTail(out);

        return any ? out.toString() : null;
    }

    private boolean substituteArray(COSArray array, Map<String, String> values,
                                    List<String> replaced) {
        StringBuilder joined = new StringBuilder();
        List<Integer> stringIndexes = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            COSBase item = array.get(i);
            if (item instanceof COSString text) {
                joined.append(text.getString());
                stringIndexes.add(i);
            }
        }

        if (stringIndexes.isEmpty()) {
            return false;
        }

        String swapped = substitute(joined.toString(), values, replaced);
        if (swapped == null) {
            return false;
        }

        array.set(stringIndexes.getFirst(), new COSString(swapped));
        // The rest are emptied rather than removed, so the array's kerning
        // numbers keep lining up with the entries around them.
        for (int i = 1; i < stringIndexes.size(); i++) {
            array.set(stringIndexes.get(i), new COSString(""));
        }
        return true;
    }
}
