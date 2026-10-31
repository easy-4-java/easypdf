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
    void alignedThreeLineColumnsRemainIndependent() throws Exception {
        PageModel page = new PageModel(1);
        for (int row = 0; row < 3; row++) {
            page.chunks.add(new PageChunk("左栏第" + row + "行", 50, 700 - row * 20, 12, false, false, 1, -1));
            page.chunks.add(new PageChunk("右栏第" + row + "行", 220, 700 - row * 20, 12, false, false, 1, -1));
        }
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(
                Collections.singletonList(page), Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).isEmpty();
        assertThat(ds.fullMarkdown()).containsSubsequence(
                "左栏第0行", "左栏第1行", "左栏第2行", "右栏第0行", "右栏第1行", "右栏第2行");
    }

    @Test
    void multipleFontRunsOnOneBaselineDoNotMergeColumns() throws Exception {
        PageModel page = new PageModel(1);
        for (int run = 0; run < 3; run++) {
            page.chunks.add(new PageChunk("左", 50 + run * 12, 700, 12, run == 1, false, 1, -1));
            page.chunks.add(new PageChunk("右", 220 + run * 12, 700, 12, run == 1, false, 1, -1));
        }
        page.chunks.add(new PageChunk("左栏末行", 50, 680, 12, false, false, 1, -1));
        page.chunks.add(new PageChunk("右栏末行", 220, 680, 12, false, false, 1, -1));
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(
                Collections.singletonList(page), Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).isEmpty();
        assertThat(ds.fullMarkdown()).containsSubsequence("左左左", "左栏末行", "右右右", "右栏末行");
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
