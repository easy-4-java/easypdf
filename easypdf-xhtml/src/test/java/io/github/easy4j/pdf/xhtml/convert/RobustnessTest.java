package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
}
