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

class LatticeTableFinderTest {

    private static final String PNG = "data:image/png;base64,"
        + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Test
    void latticeTableWithEmbeddedImageProducesCellMarkdown() throws Exception {
        // 非 Tagged 渲染（普通 htmlToPdf），强制走规则引擎
        String html = "<html><body><table border='1'>"
            + "<tr><td>名称</td><td>图示</td></tr>"
            + "<tr><td>部件A</td><td><img src='" + PNG + "'/></td></tr>"
            + "</table></body></html>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        List<PageModel> pages;
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            pages = PageModelListener.collect(doc);
        }
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.toMarkdown();
        assertThat(md).contains("| 名称 | 图示 |");
        assertThat(md).contains("部件A");
        assertThat(md).contains("![img](data:image/png;base64,");
    }

    @Test
    void latticeTablePlainTextCellsAligned() throws Exception {
        String html = "<html><body><table border='1'>"
            + "<tr><td>项目</td><td>金额</td></tr>"
            + "<tr><td>服务费</td><td>100.00</td></tr>"
            + "<tr><td>运输费</td><td>50.00</td></tr>"
            + "</table></body></html>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        List<PageModel> pages;
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            pages = PageModelListener.collect(doc);
        }
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(pages, Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).isNotEmpty();
        assertThat(ds.tables.get(0).headers.get(0)).containsExactly("项目", "金额");
        assertThat(ds.tables.get(0).rows.get(0)).containsExactly("服务费", "100.00");
    }
}
