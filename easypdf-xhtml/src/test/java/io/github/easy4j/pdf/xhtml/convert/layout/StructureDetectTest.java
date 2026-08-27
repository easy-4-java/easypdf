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
}
