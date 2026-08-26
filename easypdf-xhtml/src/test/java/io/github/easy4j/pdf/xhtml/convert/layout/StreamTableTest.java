package io.github.easy4j.pdf.xhtml.convert.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;

class StreamTableTest {

    private static List<PageModel> renderPages(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            return PageModelListener.collect(doc);
        }
    }

    @Test
    void borderlessAlignedTableBecomesPipeTable() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body><table style='border:none'>"
            + "<tr><td>列甲</td><td>列乙</td></tr>"
            + "<tr><td>值一</td><td>值二</td></tr>"
            + "<tr><td>值三</td><td>值四</td></tr>"
            + "</table></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).isNotEmpty();
        assertThat(ds.tables.get(0).headers.get(0)).containsExactly("列甲", "列乙");
        assertThat(ds.tables.get(0).rows).hasSize(2);
        assertThat(ds.tables.get(0).rows.get(0)).containsExactly("值一", "值二");
        assertThat(ds.tables.get(0).rows.get(1)).containsExactly("值三", "值四");
    }

    @Test
    void plainParagraphsNotMisdetectedAsTable() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body><p>第一段落</p><p>第二段落</p><p>第三段落</p></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).isEmpty();
        assertThat(ds.fullMarkdown()).contains("第一段落").contains("第二段落").contains("第三段落");
    }

    @Test
    void inconsistentColumnCountsDoNotTrigger() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body>"
            + "<div style='width:60%'>行一左 行一右</div>"
            + "<div style='width:60%'>行二左 行二右</div>"
            + "<div style='width:95%'>行三独占整行内容</div>"
            + "<div style='width:60%'>行四左 行四右</div>"
            + "</body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).isEmpty();
    }
}
