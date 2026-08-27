package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

/**
 * Round 3 工程健壮性与 Tagged 适配回归：
 * W3-1 角色别名归一化、W3-2 相邻重复空段去重、W3-3 页级流式提取、
 * W3-4 提取缓存、W3-5 阈值配置化与 REST 重试。
 *
 * 注：Word 导出的 Tagged PDF 样本无法离线获得，角色适配以包级可见静态方法
 * {@link PdfStructureExtractor#canonicalRole(String)} 直接单测为主，
 * 集成路径由自制 Tagged PDF（EasyPdf.markdownToPdfTagged）回归覆盖。
 */
class RobustnessTest {

    // ---------------- W3-1: 角色别名归一化 ----------------

    @Test
    void wordHeadingAliasesNormalizeToStandardRoles() {
        assertThat(PdfStructureExtractor.canonicalRole("heading 1")).isEqualTo("H1");
        assertThat(PdfStructureExtractor.canonicalRole("heading 6")).isEqualTo("H6");
        assertThat(PdfStructureExtractor.canonicalRole("h2")).isEqualTo("H2");
        assertThat(PdfStructureExtractor.canonicalRole("/h3")).isEqualTo("H3"); // 带斜杠的 PdfName 形式
        assertThat(PdfStructureExtractor.canonicalRole("标题 1")).isEqualTo("H1");
    }

    @Test
    void wordBodyListAliasesNormalizeToStandardRoles() {
        assertThat(PdfStructureExtractor.canonicalRole("p")).isEqualTo("P");
        assertThat(PdfStructureExtractor.canonicalRole("paragraph")).isEqualTo("P");
        assertThat(PdfStructureExtractor.canonicalRole("正文")).isEqualTo("P");
        assertThat(PdfStructureExtractor.canonicalRole("l")).isEqualTo("L");
        assertThat(PdfStructureExtractor.canonicalRole("list")).isEqualTo("L");
        assertThat(PdfStructureExtractor.canonicalRole("li")).isEqualTo("LI");
        assertThat(PdfStructureExtractor.canonicalRole("list item")).isEqualTo("LI");
    }

    @Test
    void wordTableAliasesNormalizeToStandardRoles() {
        assertThat(PdfStructureExtractor.canonicalRole("table")).isEqualTo("Table");
        assertThat(PdfStructureExtractor.canonicalRole("tr")).isEqualTo("TR");
        assertThat(PdfStructureExtractor.canonicalRole("table row")).isEqualTo("TR");
        assertThat(PdfStructureExtractor.canonicalRole("td")).isEqualTo("TD");
        assertThat(PdfStructureExtractor.canonicalRole("th")).isEqualTo("TH");
        assertThat(PdfStructureExtractor.canonicalRole("table header cell")).isEqualTo("TH");
    }

    @Test
    void standardRolesPassThroughAndUnknownPreserved() {
        // 标准角色名不因小写查找被改写
        assertThat(PdfStructureExtractor.canonicalRole("Table")).isEqualTo("Table");
        assertThat(PdfStructureExtractor.canonicalRole("/H1")).isEqualTo("H1");
        assertThat(PdfStructureExtractor.canonicalRole(null)).isEmpty();
        assertThat(PdfStructureExtractor.canonicalRole("")).isEmpty();
        // 未识别的自定义角色原样保留
        assertThat(PdfStructureExtractor.canonicalRole("MyCustom")).isEqualTo("MyCustom");
    }

    // ---------------- W3-3: 页级流式提取 ----------------

    private static File writePdf(File dir, String name, String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        File pdf = new File(dir, name);
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());
        return pdf;
    }

    @Test
    void perPageCallbacksCoverEveryPageWithLocalText(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "three-pages.pdf", "<html><body>"
            + "<p>第一页独有内容甲</p>"
            + "<p style='page-break-before:always'>第二页独有内容乙</p>"
            + "<p style='page-break-before:always'>第三页独有内容丙</p>"
            + "</body></html>");

        final List<Integer> pageNos = new ArrayList<Integer>();
        final List<DocumentStructure> partials = new ArrayList<DocumentStructure>();
        PdfStructureExtractor.extractPerPage(pdf, null, new PdfStructureExtractor.PageConsumer() {
            @Override public void page(int pageNo, DocumentStructure pagePartial) {
                pageNos.add(Integer.valueOf(pageNo));
                partials.add(pagePartial);
            }
        });

        assertThat(pageNos).containsExactly(1, 2, 3); // 回调计数 = 页数，页码 1..N
        String md1 = partials.get(0).fullMarkdown();
        String md2 = partials.get(1).fullMarkdown();
        String md3 = partials.get(2).fullMarkdown();
        // 第 N 页 partial 含第 N 页独有文本、不含他页文本
        assertThat(md1).contains("第一页独有内容甲").doesNotContain("乙").doesNotContain("丙");
        assertThat(md2).contains("第二页独有内容乙").doesNotContain("甲").doesNotContain("丙");
        assertThat(md3).contains("第三页独有内容丙").doesNotContain("甲").doesNotContain("乙");
        // title 继承文档标题
        assertThat(partials.get(0).title).isNotEmpty();
        assertThat(partials.get(1).title).isEqualTo(partials.get(0).title);
    }

    @Test
    void taggedSinglePageStreamsAsOneCallback(@TempDir File dir) throws Exception {
        File pdf = new File(dir, "tagged-one.pdf");
        EasyPdf.markdownToPdfTagged("# 标题页\n\n正文内容在此。\n", pdf);

        final List<Integer> pageNos = new ArrayList<Integer>();
        final List<String> texts = new ArrayList<String>();
        PdfStructureExtractor.extractPerPage(pdf, null, new PdfStructureExtractor.PageConsumer() {
            @Override public void page(int pageNo, DocumentStructure pagePartial) {
                pageNos.add(Integer.valueOf(pageNo));
                texts.add(pagePartial.fullMarkdown());
            }
        });
        assertThat(pageNos).containsExactly(1);
        assertThat(texts.get(0)).contains("标题页").contains("正文内容在此。");
    }

    @Test
    void aggregateMergesPerPagePartialsLikeWholeDocFlow() {
        // part1：隐式继承段(正文X) + 标题"第二章"；part2：隐式继承段(续流Y)
        DocumentStructure p1 = new DocumentStructure();
        p1.title = "文档标题";
        DocumentSection lead1 = new DocumentSection();
        lead1.title = "文档标题"; lead1.level = 1; lead1.content = "开篇正文";
        DocumentSection h2 = new DocumentSection();
        h2.title = "第二章"; h2.level = 2; h2.content = "";
        p1.sections = new ArrayList<DocumentSection>(Arrays.asList(lead1, h2));

        DocumentStructure p2 = new DocumentStructure();
        p2.title = "文档标题";
        DocumentSection lead2 = new DocumentSection();
        lead2.title = "文档标题"; lead2.level = 1; lead2.content = "第二章跨页续文";
        p2.sections = new ArrayList<DocumentSection>(Arrays.asList(lead2));

        DocumentStructure agg = PdfStructureExtractor.aggregate(Arrays.asList(p1, p2));
        assertThat(agg.sections).hasSize(2); // 不新增重复继承段
        assertThat(agg.sections.get(0).content).isEqualTo("开篇正文");
        // 续流并入上一节（全篇流动正文的等价形态）
        assertThat(agg.sections.get(1).title).isEqualTo("第二章");
        assertThat(agg.sections.get(1).content).isEqualTo("第二章跨页续文");
    }

    @Test
    void extractEndToEndKeepsAllPageTexts(@TempDir File dir) throws Exception {
        // 行为护栏：整篇 extract 的结果仍覆盖每页文本
        File pdf = writePdf(dir, "flow.pdf", "<html><body>"
            + "<p>首页要点记录。</p>"
            + "<p style='page-break-before:always'>次页补充说明。</p>"
            + "</body></html>");
        String md = PdfStructureExtractor.extract(pdf).fullMarkdown();
        assertThat(md).contains("首页要点记录。").contains("次页补充说明。");
    }
}
