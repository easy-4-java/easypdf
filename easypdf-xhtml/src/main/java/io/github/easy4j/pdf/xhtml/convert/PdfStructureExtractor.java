package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.tagging.IStructureNode;
import com.itextpdf.kernel.pdf.tagging.PdfMcr;
import com.itextpdf.kernel.pdf.tagging.PdfStructElem;
import com.itextpdf.kernel.pdf.tagging.PdfStructTreeRoot;
import com.itextpdf.kernel.pdf.tagging.StandardRoles;

import io.github.easy4j.pdf.xhtml.convert.layout.ExtractCache;
import io.github.easy4j.pdf.xhtml.convert.layout.PageModel;
import io.github.easy4j.pdf.xhtml.convert.layout.PageModelListener;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;
import io.github.easy4j.pdf.xhtml.convert.layout.RestLayoutAnalyzer;
import io.github.easy4j.pdf.xhtml.convert.layout.RuleLayoutAnalyzer;

/**
 * PDF 结构识别：Tagged PDF 优先（结构树角色 + mcid 关联 PageModel 取真实文本，
 * 实现自有 Tagged PDF 的无损往返），否则降级为启发式（整篇扁平文本，待 LayoutAnalyzer 分层替换）。
 */
public final class PdfStructureExtractor {

    private PdfStructureExtractor() {
    }

    public static DocumentStructure extract(File pdf) throws IOException {
        return extract(pdf, PdfExtractionProperties.defaults());
    }

    public static DocumentStructure extract(File pdf, PdfExtractionProperties props) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        if (!pdf.isFile()) {
            throw new IOException("PDF not found: " + pdf.getAbsolutePath());
        }
        PdfExtractionProperties p = props != null ? props : PdfExtractionProperties.defaults();
        String cacheKey = null;
        if (p.cacheEnabled) {
            // LRU 缓存（默认关）：key 绑定路径+修改时间+长度，文件变化自然失效
            cacheKey = ExtractCache.keyOf(pdf);
            DocumentStructure hit = ExtractCache.shared().get(cacheKey);
            if (hit != null) {
                return hit;
            }
        }
        try (ParsedDoc pd = new ParsedDoc(pdf)) {
            DocumentStructure doc = extractAll(pd, p);
            if (cacheKey != null) {
                ExtractCache.shared().put(cacheKey, doc);
            }
            return doc;
        }
    }

    // ---------------- 页级流式提取（大文件不再全量驻留） ----------------

    /** 每页回调一次：pageNo 从 1 起；REST 引擎产出整篇结果时以 pageNo=0 单次回调。 */
    public interface PageConsumer {
        void page(int pageNo, DocumentStructure pagePartial);
    }

    /**
     * 页级流式提取：逐页产出 partial {@link DocumentStructure}（title 继承文档标题，
     * sections/tables/images 为当页产物），消费方自行聚合——解析期间只驻留单页结果。
     *
     * <p>每页独立分析（无跨页统计），因此页眉剔除、跨页断词合并与全局字号聚类
     * 不适用；需要全局语义时使用 {@link #extract(File, PdfExtractionProperties)}。
     * 聚合可借助包级方法 {@link #aggregate(List)}（把后续页的隐式继承段并入上一节）。
     */
    public static void extractPerPage(File pdf, PdfExtractionProperties props, PageConsumer consumer)
            throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        if (!pdf.isFile()) {
            throw new IOException("PDF not found: " + pdf.getAbsolutePath());
        }
        try (ParsedDoc pd = new ParsedDoc(pdf)) {
            PdfExtractionProperties p = props != null ? props : PdfExtractionProperties.defaults();
            if (pd.tagged) {
                emitTaggedPerPage(pd, consumer);
                return;
            }
            boolean wantsRest = p.engine == PdfExtractionProperties.Engine.REST
                    || (p.engine == PdfExtractionProperties.Engine.AUTO && p.restEndpoint != null);
            if (wantsRest) {
                try {
                    // REST 布局服务以整份字节为输入，无法按页切分：整篇结果一次回调（pageNo=0）
                    DocumentStructure rest = new RestLayoutAnalyzer(p)
                            .analyze(java.nio.file.Files.readAllBytes(pd.source.toPath()), pd.title);
                    consumer.page(0, rest);
                    return;
                } catch (IOException e) {
                    if (p.engine == PdfExtractionProperties.Engine.REST) {
                        throw e;
                    }
                    org.slf4j.LoggerFactory.getLogger(PdfStructureExtractor.class)
                            .warn("REST layout analyzer failed, fallback to RULE: {}", e.getMessage());
                }
            }
            RuleLayoutAnalyzer analyzer = new RuleLayoutAnalyzer(props);
            for (PageModel m : pd.models) {
                consumer.page(m.pageNo,
                        analyzer.analyze(Collections.singletonList(m), null, pd.title));
            }
        }
    }

    /** Tagged 逐页：按页过滤 mcid 文本索引后走同一 walk，本页无内容的元素自然为空被跳过。 */
    private static void emitTaggedPerPage(ParsedDoc pd, PageConsumer consumer) {
        Map<String, StringBuilder> idx = buildMcidIndex(pd.models);
        List<IStructureNode> kids = pd.pdfDoc.getStructTreeRoot().getKids();
        for (int p = 1; p <= pd.pdfDoc.getNumberOfPages(); p++) {
            Map<PdfDictionary, Integer> pageNums = new HashMap<PdfDictionary, Integer>();
            pageNums.put(pd.pdfDoc.getPage(p).getPdfObject(), Integer.valueOf(p));
            Ctx ctx = new Ctx(filterIndexForPage(idx, p), pageNums, true);
            DocumentStructure part = new DocumentStructure();
            part.title = pd.title;
            for (IStructureNode child : kids) {
                walk(child, null, part, ctx);
            }
            consumer.page(p, part);
        }
    }

    /**
     * 将 {@link #extractPerPage} 的各页 partial 聚合为整篇结构。
     * 后续部分的首个"隐式继承段"（level==1、无子内容，标题等于文档标题或为空）
     * 视为跨页续流：内容并入上一节而非新建重复段——与全篇分析中 current 段持续追加的形态一致。
     * 包级可见以便单测。
     */
    static DocumentStructure aggregate(List<DocumentStructure> parts) {
        DocumentStructure agg = new DocumentStructure();
        if (parts == null || parts.isEmpty()) return agg;
        agg.title = parts.get(0).title;
        for (int k = 0; k < parts.size(); k++) {
            DocumentStructure part = parts.get(k);
            List<DocumentSection> secs = part.sections;
            int start = 0;
            if (k > 0 && secs != null && !secs.isEmpty() && isImplicitLead(secs.get(0), agg.title)) {
                DocumentSection lead = secs.get(0);
                start = 1;
                if (lead.content != null && !lead.content.isEmpty()) {
                    if (agg.sections.isEmpty()) {
                        agg.sections.add(lead);
                    } else {
                        DocumentSection tail = agg.sections.get(agg.sections.size() - 1);
                        tail.content = tail.content == null || tail.content.isEmpty()
                                ? lead.content
                                : tail.content + "\n\n" + lead.content;
                    }
                }
            }
            for (int i = start; i < secs.size(); i++) {
                agg.sections.add(secs.get(i));
            }
            agg.tables.addAll(part.tables);
            agg.images.addAll(part.images);
        }
        return agg;
    }

    private static boolean isImplicitLead(DocumentSection s, String docTitle) {
        if (s.level != 1) return false;
        if (!(s.children.isEmpty() && s.tables.isEmpty() && s.images.isEmpty())) return false;
        String t = s.title == null ? "" : s.title.trim();
        if (t.isEmpty()) return true;
        return docTitle != null && docTitle.equals(s.title);
    }

    // ---------------- 解析上下文（extract / extractPerPage 共用） ----------------

    /** 单次打开的 PDF 解析上下文：源文件引用 / 元标题 / 是否 Tagged / 全部页模型。 */
    private static final class ParsedDoc implements AutoCloseable {
        final File source;
        final PdfDocument pdfDoc;
        final String title;
        final List<PageModel> models;
        final boolean tagged;

        ParsedDoc(File pdf) throws IOException {
            this.source = pdf;
            this.pdfDoc = new PdfDocument(new PdfReader(pdf));
            String metaTitle = pdfDoc.getDocumentInfo() != null ? pdfDoc.getDocumentInfo().getTitle() : null;
            this.title = (metaTitle == null || metaTitle.isEmpty()) ? pdf.getName() : metaTitle;
            this.models = PageModelListener.collect(pdfDoc);
            PdfStructTreeRoot root = pdfDoc.getStructTreeRoot();
            boolean t = false;
            if (root != null && root.getKids() != null) {
                for (IStructureNode n : root.getKids()) {
                    if (n instanceof PdfStructElem) { t = true; break; }
                }
            }
            this.tagged = t;
        }

        @Override
        public void close() throws IOException {
            pdfDoc.close();
        }
    }

    /** 整篇语义提取：与历史行为一致——Tagged 全树优先，REST 服务次之，规则引擎兜底。 */
    private static DocumentStructure extractAll(ParsedDoc pd, PdfExtractionProperties props) throws IOException {
        PdfExtractionProperties p = props != null ? props : PdfExtractionProperties.defaults();
        DocumentStructure doc = new DocumentStructure();
        doc.title = pd.title;
        if (pd.tagged) {
            extractTaggedMcid(doc, pd.pdfDoc, pd.pdfDoc.getStructTreeRoot(), pd.models);
        }
        if (doc.sections.isEmpty() && doc.tables.isEmpty()) {
            // 非 Tagged：按引擎选择走 LayoutAnalyzer（REST 优先可回退 RULE，默认 RULE）
            boolean wantsRest = p.engine == PdfExtractionProperties.Engine.REST
                    || (p.engine == PdfExtractionProperties.Engine.AUTO && p.restEndpoint != null);
            if (wantsRest) {
                try {
                    return new RestLayoutAnalyzer(p)
                            .analyze(java.nio.file.Files.readAllBytes(pd.source.toPath()), doc.title);
                } catch (IOException e) {
                    if (p.engine == PdfExtractionProperties.Engine.REST) {
                        throw e;
                    }
                    org.slf4j.LoggerFactory.getLogger(PdfStructureExtractor.class)
                            .warn("REST layout analyzer failed, fallback to RULE: {}", e.getMessage());
                }
            }
            doc = new RuleLayoutAnalyzer(props).analyze(pd.models, null, doc.title);
        }
        return doc;
    }

    // ---------------- Tagged：结构树角色 + mcid 文本关联 ----------------

    private static void extractTaggedMcid(DocumentStructure doc, PdfDocument pdfDoc,
            PdfStructTreeRoot root, List<PageModel> models) {
        // page:mcid → 文本（按内容流顺序拼接）
        Map<String, StringBuilder> idx = buildMcidIndex(models);
        // 页对象 → 页号
        Map<PdfDictionary, Integer> pageNums = new HashMap<PdfDictionary, Integer>();
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            pageNums.put(pdfDoc.getPage(i).getPdfObject(), Integer.valueOf(i));
        }
        Ctx ctx = new Ctx(idx, pageNums);
        for (IStructureNode child : root.getKids()) {
            walk(child, null, doc, ctx);
        }
    }

    /** 全部页模型的 mcid 索引（key 形如 "pageNo:mcid"，页码无前缀歧义：均以 ':' 结尾匹配）。 */
    private static Map<String, StringBuilder> buildMcidIndex(List<PageModel> models) {
        Map<String, StringBuilder> idx = new HashMap<String, StringBuilder>();
        for (PageModel m : models) {
            for (io.github.easy4j.pdf.xhtml.convert.layout.PageChunk c : m.chunks) {
                if (c.mcid < 0) continue;
                String key = m.pageNo + ":" + c.mcid;
                StringBuilder sb = idx.get(key);
                if (sb == null) { sb = new StringBuilder(); idx.put(key, sb); }
                sb.append(c.text);
            }
        }
        return idx;
    }

    /** 取某页的 mcid 文本子索引（key 前缀 "pageNo:"，':' 边界保证 1 不误配 11）。 */
    private static Map<String, StringBuilder> filterIndexForPage(Map<String, StringBuilder> idx, int pageNo) {
        String prefix = pageNo + ":";
        Map<String, StringBuilder> sub = new HashMap<String, StringBuilder>();
        for (Map.Entry<String, StringBuilder> e : idx.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                sub.put(e.getKey(), e.getValue());
            }
        }
        return sub;
    }

    private static final class Ctx {
        final Map<String, StringBuilder> idx;
        final Map<PdfDictionary, Integer> pageNums;
        /** 流式逐页模式：idx 已按单页过滤，空内容元素直接跳过。 */
        final boolean perPage;
        Ctx(Map<String, StringBuilder> idx, Map<PdfDictionary, Integer> pageNums) {
            this(idx, pageNums, false);
        }
        Ctx(Map<String, StringBuilder> idx, Map<PdfDictionary, Integer> pageNums, boolean perPage) {
            this.idx = idx; this.pageNums = pageNums; this.perPage = perPage;
        }
    }

    private static void walk(IStructureNode node, DocumentSection parent, DocumentStructure doc, Ctx ctx) {
        if (!(node instanceof PdfStructElem)) return;
        PdfStructElem elem = (PdfStructElem) node;
        String role = normRole(elem);

        if (headingLevel(role) > 0) {
            DocumentSection sec = new DocumentSection();
            sec.title = textOf(elem, ctx);
            sec.level = headingLevel(role);
            if (ctx.perPage && sec.title.trim().isEmpty()) {
                return; // 流式逐页：该标题元素内容不在本页，跳过空壳
            }
            if (parent != null) parent.children.add(sec); else doc.sections.add(sec);
            return; // 标题元素的后代即标题自身文本，不再下潜重复
        }
        if (StandardRoles.TABLE.equals(role)) {
            DocumentTable tbl = readTable(elem, ctx);
            if (tbl != null && (tbl.headers.size() + tbl.rows.size()) > 0) {
                if (parent != null) parent.tables.add(tbl); else doc.tables.add(tbl);
            }
            return;
        }
        if (StandardRoles.L.equals(role)) {
            StringBuilder sb = new StringBuilder();
            readList(elem, 0, sb, ctx);
            String list = sb.toString().trim();
            if (!list.isEmpty()) {
                appendToCurrent(parent, doc, list);
            }
            return;
        }
        if (StandardRoles.P.equals(role)) {
            String text = textOf(elem, ctx).trim();
            if (!text.isEmpty()) {
                appendToCurrent(parent, doc, text);
            }
            return;
        }
        walkChildren(elem, parent, doc, ctx);
    }

    private static void walkChildren(PdfStructElem elem, DocumentSection parent, DocumentStructure doc, Ctx ctx) {
        if (elem.getKids() == null) return;
        for (IStructureNode child : elem.getKids()) {
            walk(child, parent, doc, ctx);
        }
    }

    private static void appendToCurrent(DocumentSection parent, DocumentStructure doc, String text) {
        DocumentSection target = parent;
        if (target == null && !doc.sections.isEmpty()) {
            target = doc.sections.get(doc.sections.size() - 1);
        }
        if (target == null) {
            target = new DocumentSection();
            target.title = "";
            target.level = 1;
            doc.sections.add(target);
        }
        if (target.content == null || target.content.isEmpty()) {
            target.content = text;
        } else {
            target.content = target.content + "\n\n" + text;
        }
    }

    /** PdfName.toString() 形如 "/H1"，归一化去斜杠并映射 Word 导出的角色别名。 */
    private static String normRole(PdfStructElem elem) {
        if (elem.getRole() == null) return "";
        return canonicalRole(elem.getRole().toString());
    }

    /** Word 等 Office 导出器常见结构角色别名 → PDF 标准角色（key 全小写）。 */
    private static final Map<String, String> ROLE_ALIASES = buildRoleAliases();

    private static Map<String, String> buildRoleAliases() {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 1; i <= 6; i++) {
            m.put("h" + i, "H" + i);
            m.put("heading" + i, "H" + i);
            m.put("heading " + i, "H" + i);
            m.put("标题" + i, "H" + i);
            m.put("标题 " + i, "H" + i);
        }
        m.put("table", StandardRoles.TABLE);
        m.put("p", StandardRoles.P);
        m.put("paragraph", StandardRoles.P);
        m.put("正文", StandardRoles.P);
        m.put("l", StandardRoles.L);
        m.put("list", StandardRoles.L);
        m.put("li", StandardRoles.LI);
        m.put("list item", StandardRoles.LI);
        m.put("tr", StandardRoles.TR);
        m.put("table row", StandardRoles.TR);
        m.put("td", StandardRoles.TD);
        m.put("th", StandardRoles.TH);
        m.put("table header cell", StandardRoles.TH);
        return m;
    }

    /**
     * 角色归一化：去斜杠 + 去首尾空白，命中别名表的映射为标准角色
     * （Word 导出常写 {@code heading 1}/{@code h1}/{@code 标题 1} 而非 {@code H1}）。
     * 未识别的自定义角色原样保留（大小写不变），与既有行为兼容。
     * 包级可见以便直接单测（Word 样本无法离线获得）。
     */
    static String canonicalRole(String raw) {
        if (raw == null) return "";
        String r = raw.trim();
        if (r.startsWith("/")) r = r.substring(1);
        if (r.isEmpty()) return "";
        String mapped = ROLE_ALIASES.get(r.toLowerCase(java.util.Locale.ROOT));
        return mapped != null ? mapped : r;
    }

    private static int headingLevel(String role) {
        if (role == null || role.length() != 2 || role.charAt(0) != 'H') return 0;
        int lv = Character.digit(role.charAt(1), 10);
        return (lv >= 1 && lv <= 6) ? lv : 0;
    }

    private static DocumentTable readTable(PdfStructElem table, Ctx ctx) {
        DocumentTable tbl = new DocumentTable();
        readRows(table, tbl, ctx);
        return tbl;
    }

    private static void readRows(PdfStructElem node, DocumentTable tbl, Ctx ctx) {
        if (node.getKids() == null) return;
        for (IStructureNode child : node.getKids()) {
            if (!(child instanceof PdfStructElem)) continue;
            PdfStructElem e = (PdfStructElem) child;
            String r = normRole(e);
            if (StandardRoles.TR.equals(r)) {
                List<String> cells = new ArrayList<String>();
                readCells(e, cells, ctx);
                if (!cells.isEmpty()) {
                    if (tbl.headers.isEmpty()) tbl.headers.add(cells);
                    else tbl.rows.add(cells);
                }
            } else if (StandardRoles.TABLE.equals(r)) {
                readRows(e, tbl, ctx); // 嵌套表拍平
            } else {
                readRows(e, tbl, ctx); // THead/TBody/TFoot 等容器
            }
        }
    }

    private static void readCells(PdfStructElem tr, List<String> cells, Ctx ctx) {
        if (tr.getKids() == null) return;
        for (IStructureNode child : tr.getKids()) {
            if (!(child instanceof PdfStructElem)) continue;
            PdfStructElem e = (PdfStructElem) child;
            String r = normRole(e);
            if (StandardRoles.TD.equals(r) || StandardRoles.TH.equals(r)) {
                cells.add(cellMarkdown(e, ctx));
            }
        }
    }

    /**
     * 单元格内容：非 Table 子元素的 mcid 文本 + 每个嵌套 Table 渲染为 GFM 子表，
     * 以 {@code <br>} 连接（保证外层 pipe 表结构不被换行破坏）。
     */
    private static String cellMarkdown(PdfStructElem td, Ctx ctx) {
        StringBuilder main = new StringBuilder();
        List<DocumentTable> subs = new ArrayList<DocumentTable>();
        if (td.getKids() != null) {
            for (IStructureNode k : td.getKids()) {
                if (!(k instanceof PdfStructElem)) continue;
                PdfStructElem ke = (PdfStructElem) k;
                if (StandardRoles.TABLE.equals(normRole(ke))) {
                    DocumentTable sub = readTable(ke, ctx);
                    if (sub != null && (sub.headers.size() + sub.rows.size()) > 0) {
                        subs.add(sub);
                    }
                } else {
                    collectText(ke, ctx, main);
                }
            }
        }
        StringBuilder cell = new StringBuilder(main.toString().trim());
        for (DocumentTable sub : subs) {
            if (cell.length() > 0) {
                cell.append("<br>");
            }
            cell.append(tableMarkdown(sub));
        }
        return cell.toString();
    }

    /** DocumentTable → GFM pipe 表文本（与 DocumentStructure.appendTable 同格式）。 */
    private static String tableMarkdown(DocumentTable t) {
        StringBuilder sb = new StringBuilder();
        if (t.headers.isEmpty()) {
            for (List<String> row : t.rows) {
                sb.append('|').append(joinCells(row)).append("|\n");
            }
            return sb.toString().trim();
        }
        for (List<String> hdr : t.headers) {
            sb.append('|').append(joinCells(hdr)).append("|\n");
        }
        sb.append('|');
        for (int i = 0; i < t.headers.get(0).size(); i++) {
            sb.append(" --- |");
        }
        sb.append('\n');
        for (List<String> row : t.rows) {
            sb.append('|').append(joinCells(row)).append("|\n");
        }
        return sb.toString().trim();
    }

    private static String joinCells(List<String> cs) {
        StringBuilder sb = new StringBuilder();
        for (String c : cs) {
            sb.append(' ').append(c == null ? "" : c.trim()).append(" |");
        }
        return sb.toString();
    }

    private static void readList(PdfStructElem list, int level, StringBuilder out, Ctx ctx) {
        if (list.getKids() == null) return;
        for (IStructureNode child : list.getKids()) {
            if (!(child instanceof PdfStructElem)) continue;
            PdfStructElem e = (PdfStructElem) child;
            String r = normRole(e);
            if (StandardRoles.LI.equals(r)) {
                // 仅取 LI 自身标签文本（跳过嵌套 L 子树，避免拍平进同一行）
                StringBuilder own = new StringBuilder();
                collectTextSkipLists(e, ctx, own);
                String text = own.toString().trim().replaceFirst("^[•·‣◦○▪\\-]\\s*", "");
                if (!text.isEmpty()) {
                    for (int i = 0; i < level; i++) {
                        out.append("  "); // 2 空格/级
                    }
                    out.append("- ").append(text).append('\n');
                }
                // 嵌套列表通常挂在 LI 的容器（LBody）下：穿透容器收集首层嵌套 L
                expandNestedLists(e, level, out, ctx);
            } else if (StandardRoles.L.equals(r)) {
                readList(e, level + 1, out, ctx); // 直接挂在 L 下的嵌套列表
            }
        }
    }

    /** 同 {@link #collectText}，但跳过嵌套 List 子树（由 readList 的递归层展开）。 */
    private static void collectTextSkipLists(PdfStructElem elem, Ctx ctx, StringBuilder out) {
        if (elem.getKids() == null) return;
        for (IStructureNode child : elem.getKids()) {
            if (child instanceof PdfMcr) {
                PdfMcr mcr = (PdfMcr) child;
                Integer page = ctx.pageNums.get(mcr.getPageObject());
                if (page == null) continue;
                StringBuilder sb = ctx.idx.get(page.intValue() + ":" + mcr.getMcid());
                if (sb != null) out.append(sb);
            } else if (child instanceof PdfStructElem) {
                PdfStructElem ce = (PdfStructElem) child;
                if (StandardRoles.L.equals(normRole(ce))) continue;
                collectTextSkipLists(ce, ctx, out);
            }
        }
    }

    /** 在 LI 子树内收集首层嵌套 L 并以 level+1 缩进展开（穿透 LBody/Div 等容器，不进入更深 LI/L）。 */
    private static void expandNestedLists(PdfStructElem node, int level, StringBuilder out, Ctx ctx) {
        if (node.getKids() == null) return;
        for (IStructureNode k : node.getKids()) {
            if (!(k instanceof PdfStructElem)) continue;
            PdfStructElem ke = (PdfStructElem) k;
            String kr = normRole(ke);
            if (StandardRoles.L.equals(kr)) {
                readList(ke, level + 1, out, ctx);
            } else if (!StandardRoles.LI.equals(kr)) {
                expandNestedLists(ke, level, out, ctx);
            }
        }
    }

    /** DFS 收集元素自身及后代全部 mcid 的文本（按结构顺序）。 */
    private static String textOf(PdfStructElem elem, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        collectText(elem, ctx, sb);
        return sb.toString();
    }

    private static void collectText(PdfStructElem elem, Ctx ctx, StringBuilder out) {
        if (elem.getKids() == null) return;
        for (IStructureNode child : elem.getKids()) {
            if (child instanceof PdfMcr) {
                PdfMcr mcr = (PdfMcr) child;
                Integer page = ctx.pageNums.get(mcr.getPageObject());
                if (page == null) continue;
                StringBuilder sb = ctx.idx.get(page.intValue() + ":" + mcr.getMcid());
                if (sb != null) out.append(sb);
            } else if (child instanceof PdfStructElem) {
                collectText((PdfStructElem) child, ctx, out);
            }
        }
    }

    // ---------------- 启发式兜底（扁平文本，待分层引擎替换） ----------------

    private static void extractHeuristic(DocumentStructure doc, PdfDocument pdfDoc) {
        DocumentSection sec = new DocumentSection();
        sec.title = doc.title != null ? doc.title : "Document";
        sec.level = 1;
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            String text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
            if (text != null && !text.isEmpty()) {
                buf.append(text.trim()).append('\n').append('\n');
            }
        }
        sec.content = buf.toString().trim();
        doc.sections.add(sec);
    }
}
