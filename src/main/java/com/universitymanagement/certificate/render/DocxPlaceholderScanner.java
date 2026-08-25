package com.universitymanagement.certificate.render;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the placeholders out of a Word design.
 *
 * <p>Only the names — not positions. A .docx describes a flowing document, not
 * a fixed page: where a line lands depends on the renderer, the installed
 * fonts and the paper size, so the same file is laid out differently by Word,
 * LibreOffice and Google Docs. A certificate has to print identically
 * everywhere, which is the problem PDF was invented to solve.
 *
 * <p>So a Word upload is accepted and inspected — the registrar is told exactly
 * which placeholders their design contains and whether they are all recognised
 * — and then asked to save it as PDF, which Word does in one step. That turns
 * a dead end into a checked hand-off.
 */
@Component
public class DocxPlaceholderScanner {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    /** @return placeholder keys, in the order they appear */
    public List<String> scan(byte[] docxBytes) throws IOException {
        Set<String> keys = new LinkedHashSet<>();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                collect(paragraph.getText(), keys);
            }

            // Certificates very often lay the details out in a borderless table,
            // and those paragraphs are not in the top-level list.
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collect(cell.getText(), keys);
                    }
                }
            }

            document.getHeaderList().forEach(header -> collect(header.getText(), keys));
            document.getFooterList().forEach(footer -> collect(footer.getText(), keys));
        }

        return new ArrayList<>(keys);
    }

    private void collect(String text, Set<String> keys) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
    }
}
