package io.github.easy4j.pdf.xhtml.convert.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;
import io.github.easy4j.pdf.xhtml.convert.PdfStructureExtractor;

/**
 * Round3 结构域检测：嵌套列表层级（W2-1）、代码块（W2-2）、题注（W2-3）、中英空格阈值配置化（W2-4）。
 */
class StructureDetectTest {

    private static List<PageModel> renderPages(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            return PageModelListener.collect(doc);
        }
    }

    private static File renderTagged(File dir, String name, String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdfTagged(html, out);
        File pdf = new File(dir, name);
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());
        return pdf;
    }

    // ---------------- W2-1 嵌套列表层级 ----------------

    @Test
    void taggedNestedListIndentsChildLevel(@TempDir File dir) throws Exception {
        File pdf = renderTagged(dir, "nested-tagged.pdf",
            "<html><body><ul><li>父级项目<ul><li>子级项目</li></ul></li></ul></body></html>");
        DocumentStructure doc = PdfStructureExtractor.extract(pdf);
        String md = doc.fullMarkdown();
        assertThat(md).containsPattern("(?m)^- 父级项目$");
        assertThat(md).containsPattern("(?m)^  - 子级项目$"); // 2 空格/级
    }

    @Test
    void rulePathNestedListIndentsAndDedentsByXStart() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body><ul><li>首要条款<ul><li>次要条款</li><li>第三条款</li></ul></li>"
            + "<li>第四条款</li></ul></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        assertThat(md).containsPattern("(?m)^- 首要条款$");
        assertThat(md).containsPattern("(?m)^  - 次要条款$"); // 子级起点 ≥ 上级 +12pt
        assertThat(md).containsPattern("(?m)^  - 第三条款$");
        assertThat(md).containsPattern("(?m)^- 第四条款$");   // 回退到上级（不残留缩进）
    }

    // ---------------- W2-2 代码块检测 ----------------

    /** 手工构造三行等宽（fontName 判定后的 mono 标记）行 → 围栏包裹、内容原样保留。 */
    private static List<PageModel> manualMonoPages() {
        PageModel page = new PageModel(1);
        page.chunks.add(new PageChunk("int x = 1;", 40f, 700f, 10f, false, true, 1, -1));
        page.chunks.add(new PageChunk("while (x > 0) {", 40f, 684f, 10f, false, true, 1, -1));
        page.chunks.add(new PageChunk("x--; }", 40f, 668f, 10f, false, true, 1, -1));
        List<PageModel> pages = new java.util.ArrayList<PageModel>();
        pages.add(page);
        return pages;
    }

    @Test
    void monoRunsFencedAsCodeBlocksVerbatim() throws Exception {
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(manualMonoPages(), Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        long fences = md.split("```", -1).length - 1;
        assertThat(fences).isGreaterThanOrEqualTo(2); // 开栏 + 闭栏
        assertThat(md).contains("\nint x = 1;\n")      // 原样保留，不做二次解析
                .contains("while (x > 0) {")
                .contains("x--; }");
        // 至少三个连续等宽行才围栏化：两行不足以成块
        PageModel two = new PageModel(1);
        two.chunks.add(new PageChunk("int x = 1;", 40f, 700f, 10f, false, true, 1, -1));
        two.chunks.add(new PageChunk("x--; ", 40f, 684f, 10f, false, true, 1, -1));
        List<PageModel> tp = new java.util.ArrayList<PageModel>();
        tp.add(two);
        DocumentStructure ds2 = new RuleLayoutAnalyzer().analyze(tp, Collections.<int[]>emptyList(), "t");
        assertThat(ds2.fullMarkdown()).doesNotContain("```");
    }

    @Test
    void preRenderedAsMonospaceBecomesFence(@TempDir java.io.File dir) throws Exception {
        File pdf = new File(dir, "code.pdf");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(
            "<html><body><p>以下为示例：</p>"
            + "<pre><code>def add(a, b):&#10;    return a + b&#10;print(add(1, 2))</code></pre>"
            + "</body></html>", out);
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(
                java.nio.file.Files.readAllBytes(pdf.toPath()))))) {
            List<PageModel> pages = PageModelListener.collect(doc);
            DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
            String md = ds.fullMarkdown();
            long fences = md.split("```", -1).length - 1;
            assertThat(fences).isGreaterThanOrEqualTo(2);
            assertThat(md).contains("print(add(1, 2))");
        }
    }

    // ---------------- W2-3 题注识别 ----------------

    @Test
    void figureAndTableCaptionsItalicized() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body><p>正文段落第一行。</p>"
            + "<p style='font-size:9px'>Figure 1: system overview</p>"
            + "<p style='font-size:9px'>图 1 系统架构示意</p>"
            + "<p style='font-size:9px'>Table 2: key metrics</p>"
            + "<p>正文段落第二行。</p></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        assertThat(md).containsPattern("(?m)^\\*Figure 1: system overview\\*$");
        assertThat(md).containsPattern("(?m)^\\*图 1 系统架构示意\\*$");
        assertThat(md).containsPattern("(?m)^\\*Table 2: key metrics\\*$");
        // 正文字号（≥ 正文阈值）的同形文本不判题注
        assertThat(md).doesNotContain("*正文段落第一行。*");
    }
}
