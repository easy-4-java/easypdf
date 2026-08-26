package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class DocumentStructureTest {

    @Test
    void documentStructureBuildsMarkdown() {
        DocumentSection h1 = new DocumentSection();
        h1.title = "合同"; h1.level = 1; h1.content = "本协议";
        DocumentSection h2 = new DocumentSection();
        h2.title = "第一章"; h2.level = 2; h2.content = "甲乙";
        h1.children = Arrays.asList(h2);

        DocumentTable tbl = new DocumentTable();
        tbl.headers = Arrays.asList(Arrays.asList("项目", "金额"));
        tbl.rows = Arrays.asList(Arrays.asList("服务费", "100.00"));

        DocumentImage img = new DocumentImage();
        img.alt = "Logo"; img.src = "data:image/png;base64,iVBOR";

        DocumentStructure doc = new DocumentStructure();
        doc.title = "测试";
        doc.sections = Arrays.asList(h1);
        doc.tables = Arrays.asList(tbl);
        doc.images = Arrays.asList(img);

        String md = doc.toMarkdown();
        assertThat(md).contains("# 合同").contains("## 第一章")
                .contains("| 项目 | 金额 |").contains("![Logo](data:image/png;base64,iVBOR)");
    }

    @Test
    void fullMarkdownSkipsDuplicateTitle() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "标题";
        DocumentSection h1 = new DocumentSection();
        h1.title = "标题"; h1.level = 1; h1.content = "x";
        doc.sections = Collections.singletonList(h1);
        assertThat(doc.fullMarkdown()).contains("# 标题").contains("x");
    }
}
