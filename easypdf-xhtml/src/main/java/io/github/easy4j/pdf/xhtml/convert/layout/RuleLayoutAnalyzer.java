package io.github.easy4j.pdf.xhtml.convert.layout;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import io.github.easy4j.pdf.xhtml.convert.DocumentSection;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;

/**
 * 规则引擎（Tier1 格线表格 + Tier2 字号聚类/分栏/列表）。
 * 当前为骨架：非 Tagged 文档输出整篇扁平文本（与旧行为等价），
 * Tier1/Tier2 任务逐步充实 analyze 内的流水线。
 */
public final class RuleLayoutAnalyzer implements LayoutAnalyzer {

    @Override
    public String name() {
        return "rule";
    }

    @Override
    public DocumentStructure analyze(List<PageModel> pages, List<int[]> taggedHeadings, String title)
            throws IOException {
        DocumentStructure doc = new DocumentStructure();
        doc.title = title;
        DocumentSection sec = new DocumentSection();
        sec.title = title != null ? title : "Document";
        sec.level = 1;
        StringBuilder buf = new StringBuilder();
        if (pages != null) {
            for (PageModel m : pages) {
                for (PageChunk c : m.chunks) {
                    buf.append(c.text);
                }
                buf.append('\n').append('\n');
            }
        }
        sec.content = buf.toString().trim();
        doc.sections.add(sec);
        return doc;
    }
}
