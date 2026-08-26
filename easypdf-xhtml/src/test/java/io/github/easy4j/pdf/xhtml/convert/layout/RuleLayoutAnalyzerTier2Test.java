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

class RuleLayoutAnalyzerTier2Test {

    private static List<PageModel> renderPages(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            return PageModelListener.collect(doc);
        }
    }

    @Test
    void headingsByFontSizeAndParagraphMerging() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body><h1>大标题</h1><h2>二级标题</h2>"
            + "<p>这是第一段落文字内容甲。</p><p>这是第二段落文字内容乙。</p></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        assertThat(md).contains("# 大标题").contains("## 二级标题");
        assertThat(md).contains("这是第一段落文字内容甲。").contains("这是第二段落文字内容乙。");
    }

    @Test
    void twoColumnReadingOrder() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body>"
            + "<div style='float:left;width:45%'>左栏第一行<br/>左栏第二行</div>"
            + "<div style='float:right;width:45%'>右栏第一行<br/>右栏第二行</div>"
            + "</body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        int leftFirst = md.indexOf("左栏第一行");
        int rightFirst = md.indexOf("右栏第一行");
        assertThat(leftFirst).isGreaterThanOrEqualTo(0);
        assertThat(rightFirst).isGreaterThanOrEqualTo(0);
        // 阅读顺序：左栏全部内容先于右栏
        assertThat(md.indexOf("左栏第二行")).isLessThan(rightFirst);
    }

    @Test
    void listMarkersDetected() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body><ul><li>首要条款</li><li>次要条款</li></ul></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        assertThat(md).contains("- 首要条款").contains("- 次要条款");
    }

    @Test
    void repeatedPageHeaderStripped() throws Exception {
        // 两页文档，每页首行重复“页眉样板”，正文不同
        String html = "<html><body>"
            + "<p style='margin:0'>页眉样板</p><p>第一页正文内容。</p>"
            + "<p style='page-break-before:always;margin:0'>页眉样板</p><p>第二页正文内容。</p>"
            + "</body></html>";
        List<PageModel> pages = renderPages(html);
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        assertThat(md).contains("第一页正文内容").contains("第二页正文内容");
        assertThat(md).doesNotContain("页眉样板");
    }

    @Test
    void coverArtTextNotMultiLevelHeadings() throws Exception {
        String cover = "这是封面宣传语这是一段很长的封面艺术文字超过八十个字符用于模拟封面大段文字场景内容继续补充到达阈值以上再加一点";
        List<PageModel> pages = renderPages(
            "<html><body><div style='font-size:42px'>" + cover + "</div>"
            + "<h2>正文小节</h2><p>正文内容。</p></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        assertThat(md).contains("## 正文小节").contains("正文内容。").contains("这是封面宣传语");
        // 封面大段文字不产生任何标题行
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^#{1,6} ").matcher(md);
        int headings = 0;
        while (m.find()) {
            int st = m.start();
            int en = md.indexOf('\n', st);
            String line = md.substring(st, en < 0 ? md.length() : en).trim();
            if (line.equals("# t")) continue; // 文档标题行（其重复输出由 Task 1 分支修复）
            headings++;
        }
        assertThat(headings).isEqualTo(1); // 仅 "## 正文小节"
    }

    @Test
    void headingLevelCappedAtThreeTiers() throws Exception {
        List<PageModel> pages = renderPages(
            "<html><body>"
            + "<div style='font-size:40px'>A40</div>"
            + "<div style='font-size:32px'>A32</div>"
            + "<div style='font-size:26px'>A26</div>"
            + "<div style='font-size:21px'>A21</div>"
            + "<div style='font-size:18px'>A18</div>"
            + "<p>正文。</p></body></html>");
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, Collections.<int[]>emptyList(), "t");
        String md = ds.fullMarkdown();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^#{1,6} A").matcher(md);
        int headings = 0;
        while (m.find()) headings++;
        assertThat(headings).isLessThanOrEqualTo(3); // 最多 3 档标题
        assertThat(md).doesNotContain("# A21").doesNotContain("# A18"); // 降为正文
        assertThat(md).contains("A21").contains("A18"); // 内容仍在
    }
}
