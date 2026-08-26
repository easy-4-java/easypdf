package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.List;

import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;

/**
 * 布局分析器 SPI：把 PageModel（单遍收集的页面元素）重建为 DocumentStructure。
 * 内置 {@link RuleLayoutAnalyzer}（Tier1+2 规则引擎）；
 * ML 布局服务（docling/MinerU 类）通过 REST 实现接入（Tier3）。
 */
public interface LayoutAnalyzer {

    String name();

    /**
     * 分析整份文档。
     *
     * @param pages          每页的 PageModel（含 chunks/images/strokes）
     * @param taggedHeadings Tagged PDF 的标题提示（元素为 {page, level}，规则引擎用作字号聚类先验；可空）
     * @param title          文档标题（来自元数据或文件名）
     */
    DocumentStructure analyze(List<PageModel> pages, List<int[]> taggedHeadings, String title)
            throws java.io.IOException;
}
