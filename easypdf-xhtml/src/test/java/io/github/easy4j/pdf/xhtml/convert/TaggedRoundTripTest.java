package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
