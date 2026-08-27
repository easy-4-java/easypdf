package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

class DocumentSummaryBuilderTest {

    @Test
    void buildRejectsNullFile() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> DocumentSummaryBuilder.build(null, PdfExtractionProperties.defaults()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildReturnsSummaryWithSectionTree() throws Exception {
        File pdf = writeTempPdf("<html><body>"
            + "<h1>总章</h1><p>正文一二三。</p>"
            + "<h2>分章</h2><p>四五六。</p>"
            // 规则引擎仅识别无边框且≥3 行的对齐表（与 StreamTableTest 的已验证夹具一致）
            + "<table style='border:none'>"
            + "<tr><td>列甲</td><td>列乙</td></tr>"
            + "<tr><td>值一</td><td>值二</td></tr>"
            + "<tr><td>值三</td><td>值四</td></tr>"
            + "</table>"
            + "</body></html>");
        DocumentSummary sum = DocumentSummaryBuilder.build(pdf, PdfExtractionProperties.defaults());
        assertThat(sum.title).isEqualTo("总章");
        assertThat(sum.sections).hasSize(2);
        assertThat(sum.sections.get(0).title).isEqualTo("总章");
        assertThat(sum.sections.get(0).level).isEqualTo(1);
        assertThat(sum.sections.get(1).level).isEqualTo(2);
        assertThat(sum.totalTables).isEqualTo(1);
    }

    private File writeTempPdf(String html) throws Exception {
        File f = File.createTempFile("r4test", ".pdf");
        f.deleteOnExit();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        Files.write(f.toPath(), out.toByteArray());
        return f;
    }
}
