package io.github.easy4j.pdf.xhtml.convert;

import java.util.Arrays;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Markdown 与 HTML/纯文本之间的转换工具。
 */
public final class MarkdownConverter {

    private static final MutableDataSet OPTIONS = new MutableDataSet();

    static {
        OPTIONS.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()));
        OPTIONS.set(HtmlRenderer.SOFT_BREAK, "<br />");
    }

    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    private MarkdownConverter() {
    }

    /** 将 Markdown 文本渲染为 HTML 字符串（CommonMark + GFM 表格/删除线/任务列表）。 */
    public static String mdToHtml(String markdown) {
        return RENDERER.render(PARSER.parse(markdown));
    }

    /**
     * 将 PDF 提取出的纯文本整理为简单 Markdown：保留段落（空行分隔）与换行结构。
     * 尽力而为——PDF 无结构信息，仅做可读性整理，对齐 pandoc 的定位。
     */
    public static String textToMarkdown(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        return normalized.replaceAll("\n{3,}", "\n\n");
    }
}
