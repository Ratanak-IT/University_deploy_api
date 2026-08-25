package com.universitymanagement.certificate.render;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CertificateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    /** Every key a template may use, with a short description for the editor. */
    public static final Map<String, String> SUPPORTED = new LinkedHashMap<>() {{
        put("studentName", "Student's full name");
        put("studentCode", "Student ID, e.g. STU-001");
        put("programName", "Programme the student is enrolled in");
        put("degreeLevel", "Bachelor, Master, …");
        put("yearLevel", "Year of study");
        put("academicYear", "e.g. 2025-2026");
        put("cumulativeGpa", "Official CGPA, posted grades only");
        put("creditsEarned", "Credits earned");
        put("certificateNumber", "Unique reference of this certificate");
        put("verificationCode", "Code the public verification page checks");
        put("verificationUrl", "Full URL of the verification page");
        put("issueDate", "Date this certificate was issued");
        put("universityName", "Institution name");
    }};

    public String render(String templateHtml, Map<String, String> values) {
        if (templateHtml == null) {
            return "";
        }

        Matcher matcher = PLACEHOLDER.matcher(templateHtml);
        StringBuilder out = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            // An unknown placeholder is left visible rather than blanked, so a
            // typo shows up in the preview instead of producing a certificate
            // with a silent gap where a name should be.
            String replacement = value != null ? escape(value) : matcher.group(0);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);

        return out.toString();
    }

    /** Which placeholders a template uses that this renderer does not know. */
    public java.util.List<String> unknownPlaceholders(String templateHtml) {
        if (templateHtml == null) {
            return java.util.List.of();
        }
        java.util.List<String> unknown = new java.util.ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(templateHtml);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!SUPPORTED.containsKey(key) && !unknown.contains(key)) {
                unknown.add(key);
            }
        }
        return unknown;
    }

    private String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
