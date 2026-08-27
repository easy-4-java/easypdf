package io.github.easy4j.pdf.xhtml.convert;

import java.util.ArrayList;
import java.util.List;

public final class DocumentSection {
    public String title;
    public int level;
    /**
     * 该 section 起始所在页号：{@link PdfStructureExtractor#extractPerPage} 流式回调前写入
     * （含 children 递归）；整篇提取路径不标注，缺省 0 表示页锚点未知。
     */
    public int page;
    public String content = "";
    public List<DocumentSection> children = new ArrayList<DocumentSection>();
    public List<DocumentTable> tables = new ArrayList<DocumentTable>();
    public List<DocumentImage> images = new ArrayList<DocumentImage>();
}
