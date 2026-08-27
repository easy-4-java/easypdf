package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

/**
 * Agent API：单遍流式收集 PDF 元数据，输出 {@link DocumentSummary} 摘要
 * （页数 / 字符数 / 表格数 / 图片数 + level≤2 章节骨架），不驻留正文。
 * 基于 {@link PdfStructureExtractor#extractPerPage} 逐页消费，解析期间只保留单页结果，
 * 大文件无需全量加载即可拿到结构概览。
 */
public final class DocumentSummaryBuilder {

    private DocumentSummaryBuilder() {
    }

    public static DocumentSummary build(File pdf, PdfExtractionProperties props) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        PdfExtractionProperties p = props != null ? props : PdfExtractionProperties.defaults();
        DocumentSummary sum = new DocumentSummary();
        PdfStructureExtractor.extractPerPage(pdf, p, new PdfStructureExtractor.PageConsumer() {
            @Override
            public void page(int pageNo, DocumentStructure partial) {
                sum.totalPages = Math.max(sum.totalPages, pageNo);
                if (partial == null) {
                    return;
                }
                // 文档标题兜底：取首页回调的元标题（末尾若存在 level-1 章节标题则被覆盖为更精确值）
                if (sum.title == null && pageNo <= 1) {
                    sum.title = partial.title;
                }
                for (DocumentSection sec : partial.sections) {
                    DocumentSummarySection ss = toSummarySection(sec, pageNo);
                    if (sec.level <= 2 && ss.title != null && !ss.title.isEmpty()) {
                        sum.sections.add(ss);
                    }
                    sum.totalChars += ss.charCount;
                    sum.totalTables += ss.tableCount;
                    sum.totalImages += ss.imageCount;
                }
                // 规则引擎把表格/图片放在整篇结果的顶层（不在 section 内）：同样计入总量
                if (partial.tables != null) {
                    sum.totalTables += partial.tables.size();
                }
                if (partial.images != null) {
                    sum.totalImages += partial.images.size();
                }
            }
        });
        // 文档标题：取首个 level-1 章节标题（比文件名/元数据更精确）
        for (DocumentSummarySection s : sum.sections) {
            if (s.level == 1 && s.title != null && !s.title.isEmpty()) {
                sum.title = s.title;
                break;
            }
        }
        return sum;
    }

    private static DocumentSummarySection toSummarySection(DocumentSection sec, int pageNo) {
        DocumentSummarySection ss = new DocumentSummarySection();
        ss.title = sec.title;
        ss.level = sec.level;
        ss.pageNo = pageNo;
        ss.charCount = sec.content == null ? 0 : sec.content.length();
        ss.tableCount = sec.tables == null ? 0 : sec.tables.size();
        ss.imageCount = sec.images == null ? 0 : sec.images.size();
        return ss;
    }
}
