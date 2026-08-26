package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownConverterTest {

    @Test
    void mdToHtmlRendersHeadingAndParagraph() {
        String html = MarkdownConverter.mdToHtml("# 标题\n\n正文内容");
        assertThat(html).contains("<h1>").contains("标题").contains("正文内容");
    }

    @Test
    void mdToHtmlRendersTable() {
        String html = MarkdownConverter.mdToHtml("| a | b |\n|---|---|\n| 1 | 2 |");
        assertThat(html).contains("<table>").contains("<th>").contains("a");
    }

    @Test
    void mdToHtmlRendersCodeBlockAndList() {
        String md = "```java\nint x = 1;\n```\n\n- item1\n- item2";
        String html = MarkdownConverter.mdToHtml(md);
        assertThat(html).contains("<pre>").contains("int x = 1;").contains("<ul>").contains("item1");
    }

    @Test
    void textToMarkdownKeepsLinesAndParagraphs() {
        String md = MarkdownConverter.textToMarkdown("第一行\n\n第二行");
        assertThat(md).contains("第一行").contains("第二行");
    }
}
