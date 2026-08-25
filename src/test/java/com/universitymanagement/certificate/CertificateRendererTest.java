package com.universitymanagement.certificate;

import com.universitymanagement.certificate.render.CertificateRenderer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateRendererTest {

    private final CertificateRenderer renderer = new CertificateRenderer();

    @Test
    void substitutesKnownPlaceholders() {
        String out = renderer.render(
                "<p>{{studentName}} — {{studentCode}}</p>",
                Map.of("studentName", "Sok Dara", "studentCode", "STU-001"));

        assertEquals("<p>Sok Dara — STU-001</p>", out);
    }

    @Test
    void toleratesSpacesInsideTheBraces() {
        assertEquals("Sok Dara",
                renderer.render("{{ studentName }}", Map.of("studentName", "Sok Dara")));
    }

    @Test
    void escapesValuesSoAStudentNameCannotInjectMarkup() {
        String out = renderer.render(
                "<p>{{studentName}}</p>",
                Map.of("studentName", "<script>steal()</script>"));

        assertFalse(out.contains("<script>"),
                "a value must never be able to introduce markup into the document");
        assertTrue(out.contains("&lt;script&gt;"));
    }

    @Test
    void escapesAmpersandsAndQuotesToo() {
        String out = renderer.render("{{programName}}",
                Map.of("programName", "Arts & Design \"Honours\""));

        assertEquals("Arts &amp; Design &quot;Honours&quot;", out);
    }

    @Test
    void anUnknownPlaceholderStaysVisibleRatherThanVanishing() {
        String out = renderer.render("<p>{{nickname}}</p>", Map.of("studentName", "Sok Dara"));

        assertEquals("<p>{{nickname}}</p>", out,
                "a typo must show up in the preview, not leave a silent gap");
    }

    @Test
    void reportsPlaceholdersItDoesNotKnow() {
        var unknown = renderer.unknownPlaceholders("{{studentName}} {{nickname}} {{nickname}}");

        assertEquals(1, unknown.size(), "duplicates are reported once");
        assertEquals("nickname", unknown.getFirst());
    }

    @Test
    void aTemplateWithNoPlaceholdersPassesThroughUnchanged() {
        String html = "<h1>Certificate of Completion</h1>";
        assertEquals(html, renderer.render(html, Map.of()));
    }

    @Test
    void everySupportedKeyIsDocumented() {
        // The editor lists these to the person writing the template, so an
        // undocumented key would be one nobody could discover.
        assertTrue(CertificateRenderer.SUPPORTED.containsKey("studentName"));
        assertTrue(CertificateRenderer.SUPPORTED.containsKey("certificateNumber"));
        assertTrue(CertificateRenderer.SUPPORTED.containsKey("verificationUrl"));
        CertificateRenderer.SUPPORTED.values()
                .forEach(description -> assertFalse(description.isBlank()));
    }
}
