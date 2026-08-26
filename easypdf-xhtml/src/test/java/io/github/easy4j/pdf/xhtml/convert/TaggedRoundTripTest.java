package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

class TaggedRoundTripTest {

    @Test
    void roundTripPreservesHeadingsTableAndList(@TempDir File dir) throws Exception {
        String md = "# 合同标题\n\n"
            + "## 第一章 双方义务\n\n甲方应当守约。乙方应当配合。\n\n"
            + "| 项目 | 金额 |\n|---|---|\n| 服务费 | 100.00 |\n| 运输费 | 50.00 |\n\n"
            + "- 首要条款\n- 次要条款\n";
        File pdf = new File(dir, "rt.pdf");
        EasyPdf.markdownToPdfTagged(md, pdf);

        DocumentStructure doc = EasyPdf.pdfToStructured(pdf);
        String back = doc.toMarkdown();

        assertThat(back).contains("# 合同标题").contains("## 第一章");
        assertThat(back).contains("甲方应当").contains("乙方应当");
        assertThat(back).contains("| 项目 | 金额 |").contains("100.00").contains("50.00");
        assertThat(back).contains("首要条款").contains("次要条款");
    }

    @Test
    void nestedTableLandsInParentCell(@TempDir File dir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdfTagged(
            "<html><body><table><tr><th>外列</th><th>明细</th></tr>"
          + "<tr><td>汇总</td><td><table><tr><td>子项</td><td>1</td></tr></table></td></tr>"
          + "</table></body></html>", out);
        File pdf = new File(dir, "nested.pdf");
        Files.write(pdf.toPath(), out.toByteArray());

        DocumentStructure doc = EasyPdf.pdfToStructured(pdf);
        assertThat(doc.tables).hasSize(1);
        // 外层 2 列 1 数据行（嵌套不得拍平成幽灵行）；第二个 cell 含子表内容（子项/1）
        assertThat(doc.tables.get(0).headers.get(0)).containsExactly("外列", "明细");
        assertThat(doc.tables.get(0).rows).hasSize(1);
        assertThat(doc.tables.get(0).rows.get(0).get(1)).contains("子项").contains("1")
            .contains("|"); // 子表以 pipe-table 文本并入父 cell
    }
}

