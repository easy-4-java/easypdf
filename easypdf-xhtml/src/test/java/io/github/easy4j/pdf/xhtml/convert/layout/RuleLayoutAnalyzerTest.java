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

class RuleLayoutAnalyzerTest {

    @Test
    void analyzeProducesFlatSectionFromChunks() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf("<html><body><p>左文一段</p><p>右文一段</p></body></html>", out);
        List<PageModel> pages;
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            pages = PageModelListener.collect(doc);
        }
        RuleLayoutAnalyzer analyzer = new RuleLayoutAnalyzer();
        assertThat(analyzer.name()).isEqualTo("rule");
        DocumentStructure ds = analyzer.analyze(pages, Collections.<int[]>emptyList(), "测试文档");
        assertThat(ds.sections).isNotEmpty();
        assertThat(ds.fullMarkdown()).contains("测试文档").contains("左文一段").contains("右文一段");
    }

    @Test
    void analyzeHandlesEmptyPages() throws Exception {
        RuleLayoutAnalyzer analyzer = new RuleLayoutAnalyzer();
        DocumentStructure ds = analyzer.analyze(null, null, "空");
        assertThat(ds.sections).isNotEmpty();
    }

    @Test
    void propertiesDefaultToAutoRule() {
        PdfExtractionProperties p = PdfExtractionProperties.defaults();
        assertThat(p.engine).isEqualTo(PdfExtractionProperties.Engine.AUTO);
        assertThat(p.restEndpoint).isNull();
        assertThat(p.restTimeoutMillis).isEqualTo(10000);
    }
}
