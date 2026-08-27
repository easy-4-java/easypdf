
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

class DeepNestedPdfTest {

    @Test
    void largeTextPdfDoesNotHang(@TempDir File dir) throws Exception {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            sb.append("<p>line ").append(i).append(" benchmark verification content 中英文混排</p>");
        }
        sb.append("</body></html>");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(sb.toString(), out);
        File pdf = new File(dir, "deep.pdf");
        Files.write(pdf.toPath(), out.toByteArray());
        long t0 = System.currentTimeMillis();
        DocumentStructure ds = PdfStructureExtractor.extract(pdf, PdfExtractionProperties.defaults());
        long dt = System.currentTimeMillis() - t0;
        assertThat(dt).as("extraction time").isLessThan(10000); // 10s max
        assertThat(ds).isNotNull();
        assertThat(ds.sections).isNotEmpty();
        assertThat(ds.sections.get(0).content).contains("line 0");
    }
}
