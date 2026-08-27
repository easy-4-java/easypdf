package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

/**
 * Round4-P2 Task 3：报告式提取 {@link PdfStructureExtractor#extractWithReport}。
 * 核心语义：永不抛异常——成功填 document 与统计计数；失败填 error（分类码）；
 * 空文本层 PDF 以 warnings 提示而非失败。
 */
class ExtractionReportTest {

    private static File writePdf(File dir, String name, String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        File pdf = new File(dir, name);
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());
        return pdf;
    }

    @Test
    void successReportFillsDocumentCountsAndDuration(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "ok.pdf",
            "<html><body><h1>报告标题</h1><p>第一章正文内容，用于验证统计计数。</p></body></html>");

        ExtractReport r = PdfStructureExtractor.extractWithReport(pdf, PdfExtractionProperties.defaults());

        assertThat(r.success).isTrue();
        assertThat(r.error).isNull();
        assertThat(r.document).isNotNull();
        assertThat(r.pages).isEqualTo(1);
        assertThat(r.durationMillis).isGreaterThanOrEqualTo(0);
        assertThat(r.chars).isGreaterThan(0);
        assertThat(r.tables).isGreaterThanOrEqualTo(0);
        assertThat(r.images).isGreaterThanOrEqualTo(0);
        assertThat(r.warnings).isEmpty(); // 有文本层：无告警
    }

    @Test
    void missingFileMapsToNotFoundAndNeverThrows(@TempDir File dir) {
        File missing = new File(dir, "no-such.pdf");

        ExtractReport r = PdfStructureExtractor.extractWithReport(missing, null);

        assertThat(r.success).isFalse();
        assertThat(r.error).isNotNull();
        assertThat(r.error.getCode()).isEqualTo(ExtractionException.Code.NOT_FOUND);
        assertThat(r.document).isNull();
        assertThat(r.durationMillis).isGreaterThanOrEqualTo(0);
    }

    @Test
    void emptyTextPdfStillSucceedsWithNoTextWarning(@TempDir File dir) throws Exception {
        // 无任何文本内容的单页 PDF（仅一个不可见色块）
        File pdf = writePdf(dir, "blank.pdf",
            "<html><body><div style=\"width:100pt;height:100pt;background:#eeeeee\"></div></body></html>");

        ExtractReport r = PdfStructureExtractor.extractWithReport(pdf, null);

        assertThat(r.success).isTrue(); // 提取本身成功，不算失败
        assertThat(r.error).isNull();
        assertThat(r.document).isNotNull();
        assertThat(r.chars).isEqualTo(0);
        assertThat(r.warnings).anySatisfy(w -> assertThat(w).contains("no text"));
    }
}
