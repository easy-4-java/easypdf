package io.github.easy4j.pdf.xhtml.convert;

import java.util.ArrayList;
import java.util.List;

/**
 * 报告式提取的结果载体：{@link PdfStructureExtractor#extractWithReport} 的返回值。
 * 契约为"永不抛异常"——调用方读字段即可，无需 try/catch：
 * 成功时 {@link #success}=true 且 {@link #document} 非 null；失败时 {@link #error} 携带分类码
 * （{@link ExtractionException.Code}），已统计的计数保留供部分诊断。
 */
public final class ExtractReport {

    /** 提取成功时的文档结构；失败时为 null。 */
    public DocumentStructure document;

    /** 失败原因（按 {@link ExtractionException.Code} 分级）；成功时为 null。 */
    public ExtractionException error;

    /** true 表示提取成功（document 可用）；false 表示失败（看 {@link #error}）。 */
    public boolean success;

    /**
     * 页数：从 section 页锚点推断的最大页号；
     * 整篇 Tagged 路径不写锚点（缺省 0）时下限记 1（PDF 至少一页）。
     */
    public int pages;

    /** 全部 section（含子级递归）正文总字符数。 */
    public long chars;

    /** 表格总数（文档级 + section 内递归）。 */
    public long tables;

    /** 图片总数（文档级 + section 内递归）。 */
    public long images;

    /** 提取耗时（毫秒）。 */
    public long durationMillis;

    /** 非致命提示：如无文本层 PDF 追加 "no text extracted"。 */
    public List<String> warnings = new ArrayList<String>();
}
