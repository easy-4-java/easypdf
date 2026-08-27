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

    @Test
    void fullMarkdownDeduplicatesDocTitleAndFirstHeading() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "合同";
        DocumentSection h1 = new DocumentSection();
        h1.title = "合同"; h1.level = 1; h1.content = "正文";
        doc.sections = Collections.singletonList(h1);
        String md = doc.fullMarkdown();
        assertThat(md).contains("# 合同").contains("正文");
        assertThat(md.indexOf("# 合同")).isEqualTo(md.lastIndexOf("# 合同")); // 只出现一次
    }

    @Test
    void fullMarkdownKeepsTitleWhenFirstSectionDiffers() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "文档元标题";
        DocumentSection h1 = new DocumentSection();
        h1.title = "章标题"; h1.level = 1; h1.content = "x";
        doc.sections = Collections.singletonList(h1);
        assertThat(doc.fullMarkdown()).contains("# 文档元标题").contains("# 章标题");
    }

    @Test
    void adjacentDuplicateEmptySectionsCollapseToOne() {
        // 两个同名 level-1 空段相邻，只输出一次（W3-2：去重扩展到任意相邻重复）
        DocumentStructure doc = new DocumentStructure();
        DocumentSection s1 = new DocumentSection();
        s1.title = "附录"; s1.level = 1; // content 为空
        DocumentSection s2 = new DocumentSection();
        s2.title = "附录"; s2.level = 1; // content 为空
        doc.sections = Arrays.asList(s1, s2);
        String md = doc.toMarkdown();
        assertThat(md.indexOf("# 附录")).isEqualTo(md.lastIndexOf("# 附录"));
    }

    @Test
    void adjacentDuplicateNestedEmptyChildrenCollapseToo() {
        // 嵌套子级同样应用相邻去重
        DocumentSection parent = new DocumentSection();
        parent.title = "卷一"; parent.level = 1;
        DocumentSection c1 = new DocumentSection();
        c1.title = "附录"; c1.level = 2;
        DocumentSection c2 = new DocumentSection();
        c2.title = "附录"; c2.level = 2;
        parent.children = Arrays.asList(c1, c2);
        DocumentStructure doc = new DocumentStructure();
        doc.sections = Collections.singletonList(parent);
        String md = doc.toMarkdown();
        assertThat(md.indexOf("## 附录")).isEqualTo(md.lastIndexOf("## 附录"));
    }

    @Test
    void duplicateTitleWithContentIsNotDropped() {
        // 同名但后段携带内容的相邻段不去重（防误删真实内容）
        DocumentStructure doc = new DocumentStructure();
        DocumentSection s1 = new DocumentSection();
        s1.title = "术语表"; s1.level = 1; s1.content = "";
        DocumentSection s2 = new DocumentSection();
        s2.title = "术语表"; s2.level = 1; s2.content = "PDF：便携式文档格式";
        doc.sections = Arrays.asList(s1, s2);
        String md = doc.toMarkdown();
        assertThat(md).contains("PDF：便携式文档格式");
        assertThat(md.indexOf("# 术语表")).isNotEqualTo(md.lastIndexOf("# 术语表"));
    }
}
