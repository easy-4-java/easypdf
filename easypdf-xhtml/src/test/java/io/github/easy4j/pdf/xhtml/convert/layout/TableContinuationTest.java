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
import io.github.easy4j.pdf.xhtml.convert.DocumentTable;

/**
 * W1 表格域：跨页表格续接（Task W1-1）。
 * 夹具用 HtmlPdfConverter.htmlToPdf 渲染：两页各放同构 border=1 格线表，
 * 以 page-break-before:always 分页；非 Tagged 路径强制走 RuleLayoutAnalyzer 规则引擎。
 */
class TableContinuationTest {

    private static List<PageModel> renderPages(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            return PageModelListener.collect(doc);
        }
    }

    @Test
    void continuedTableMergedAcrossPages() throws Exception {
        // 两页同构表：第二页无独立表头证据（首行普通 td、字号=正文）且列数相同、
        // 首末列 x 对齐 → 合并为一张；续表首行并入 rows 而非 headers。
        String html = "<html><body>"
            + "<table border='1'>"
            + "<tr><td>名称</td><td>金额</td></tr>"
            + "<tr><td>服务费</td><td>100.00</td></tr>"
            + "<tr><td>咨询费</td><td>200.00</td></tr>"
            + "</table>"
            + "<p style='margin:0'>第一页附注说明文字。</p>"
            + "<p style='page-break-before:always;margin:0'></p>"
            + "<table border='1'>"
            + "<tr><td>运输费</td><td>50.00</td></tr>"
            + "<tr><td>仓储费</td><td>80.00</td></tr>"
            + "</table>"
            + "<p style='margin:0'>第二页附注说明文字。</p>"
            + "</body></html>";
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(renderPages(html), Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).hasSize(1);
        DocumentTable t = ds.tables.get(0);
        assertThat(t.headers).hasSize(1);
        assertThat(t.headers.get(0)).containsExactly("名称", "金额");
        assertThat(t.rows).hasSize(4); // 首页 2 行数据 + 续页 2 行数据（续表首行并入 rows）
        assertThat(t.rows.get(0)).containsExactly("服务费", "100.00");
        assertThat(t.rows.get(1)).containsExactly("咨询费", "200.00");
        assertThat(t.rows.get(2)).containsExactly("运输费", "50.00");
        assertThat(t.rows.get(3)).containsExactly("仓储费", "80.00");
    }

    @Test
    void differingColumnCountStaysSeparate() throws Exception {
        // 第二页列数不同 → 不满足续接条件，保持两张独立表
        String html = "<html><body>"
            + "<table border='1'>"
            + "<tr><td>名称</td><td>金额</td></tr>"
            + "<tr><td>服务费</td><td>100.00</td></tr>"
            + "</table>"
            + "<p style='margin:0'>第一页附注说明文字。</p>"
            + "<p style='page-break-before:always;margin:0'></p>"
            + "<table border='1'>"
            + "<tr><td>项目</td><td>数量</td><td>备注</td></tr>"
            + "<tr><td>耗材</td><td>12</td><td>-</td></tr>"
            + "</table>"
            + "<p style='margin:0'>第二页附注说明文字。</p>"
            + "</body></html>";
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(renderPages(html), Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).hasSize(2);
        assertThat(ds.tables.get(0).headers.get(0)).containsExactly("名称", "金额");
        assertThat(ds.tables.get(1).headers.get(0)).containsExactly("项目", "数量", "备注");
    }

    @Test
    void colspanWideCellKeepsPlaceholderColumns() throws Exception {
        // 横跨两列的合并格：文本放首个覆盖列，其余列以空串占位，保持 GFM 列数对齐
        String html = "<html><body><table border='1'>"
            + "<tr><td>A甲</td><td>A乙</td><td>A丙</td></tr>"
            + "<tr><td colspan='2'>跨两列内容较长的合并单元格文本</td><td>B丙</td></tr>"
            + "<tr><td>C甲</td><td>C乙</td><td>C丙</td></tr>"
            + "</table></body></html>";
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(renderPages(html), Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).hasSize(1);
        DocumentTable t = ds.tables.get(0);
        assertThat(t.headers.get(0)).containsExactly("A甲", "A乙", "A丙");
        assertThat(t.rows.get(0)).containsExactly("跨两列内容较长的合并单元格文本", "", "B丙");
        assertThat(t.rows.get(1)).containsExactly("C甲", "C乙", "C丙");
        // Markdown 输出保形：占位空列保留管道分隔（行尾“||”为既有渲染形态，
        // 归 W3 的 DocumentStructure 域，此处只锁列占位与顺序）
        String md = ds.toMarkdown();
        assertThat(md).contains("| A甲 | A乙 | A丙 ")
                      .contains("| 跨两列内容较长的合并单元格文本 |  | B丙 ")
                      .contains("| C甲 | C乙 | C丙 ");
    }

    @Test
    void rowspanCellStaysInFirstBandWithEmptyPlaceholders() throws Exception {
        // 纵跨两行的合并格：垂直居中渲染的文本必须归入首行带，余行留空占位；
        // 单元格内多行文本按视觉自上而下拼接。
        String html = "<html><body><table border='1'>"
            + "<tr><td rowspan='2'>纵跨单元格文本</td><td>R1B</td></tr>"
            + "<tr><td>R2B高行内容较多较多较多<br/>第二行使下方变高</td></tr>"
            + "</table></body></html>";
        DocumentStructure ds = new RuleLayoutAnalyzer()
                .analyze(renderPages(html), Collections.<int[]>emptyList(), "t");
        assertThat(ds.tables).hasSize(1);
        DocumentTable t = ds.tables.get(0);
        assertThat(t.headers.get(0)).containsExactly("纵跨单元格文本", "R1B");
        assertThat(t.rows.get(0)).containsExactly("", "R2B高行内容较多较多较多第二行使下方变高");
    }
}
