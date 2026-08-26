package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
        if (props == null) {
            props = PdfExtractionProperties.defaults();
        }
        if (!pdf.isFile()) {
            throw new IOException("PDF not found: " + pdf.getAbsolutePath());
        }
        DocumentStructure doc = new DocumentStructure();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdf))) {
            String metaTitle = pdfDoc.getDocumentInfo() != null ? pdfDoc.getDocumentInfo().getTitle() : null;
            doc.title = (metaTitle == null || metaTitle.isEmpty()) ? pdf.getName() : metaTitle;

            List<PageModel> models = PageModelListener.collect(pdfDoc);
            PdfStructTreeRoot root = pdfDoc.getStructTreeRoot();
            boolean tagged = false;
            if (root != null && root.getKids() != null) {
                for (IStructureNode n : root.getKids()) {
                    if (n instanceof PdfStructElem) { tagged = true; break; }
                }
            }
            if (tagged) {
                extractTaggedMcid(doc, pdfDoc, root, models);
            }
            if (doc.sections.isEmpty() && doc.tables.isEmpty()) {
                // 非 Tagged：按引擎选择走 LayoutAnalyzer（REST 优先可回退 RULE，默认 RULE）
                boolean wantsRest = props.engine == PdfExtractionProperties.Engine.REST
                        || (props.engine == PdfExtractionProperties.Engine.AUTO && props.restEndpoint != null);
                if (wantsRest) {
                    try {
                        return new RestLayoutAnalyzer(props)
                                .analyze(java.nio.file.Files.readAllBytes(pdf.toPath()), doc.title);
                    } catch (IOException e) {
                        if (props.engine == PdfExtractionProperties.Engine.REST) {
                            throw e;
                        }
                        org.slf4j.LoggerFactory.getLogger(PdfStructureExtractor.class)
                                .warn("REST layout analyzer failed, fallback to RULE: {}", e.getMessage());
                    }
                }
                doc = new RuleLayoutAnalyzer().analyze(models, null, doc.title);
            }
        }
        return doc;
    }

    // ---------------- Tagged：结构树角色 + mcid 文本关联 ----------------

    private static void extractTaggedMcid(DocumentStructure doc, PdfDocument pdfDoc,
            PdfStructTreeRoot root, List<PageModel> models) {
        // page:mcid → 文本（按内容流顺序拼接）
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

    private static final class Ctx {
        final Map<String, StringBuilder> idx;
        final Map<PdfDictionary, Integer> pageNums;
        Ctx(Map<String, StringBuilder> idx, Map<PdfDictionary, Integer> pageNums) {
            this.idx = idx; this.pageNums = pageNums;
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
            readList(elem, sb, ctx);
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

    /** PdfName.toString() 形如 "/H1"，归一化去斜杠。 */
    private static String normRole(PdfStructElem elem) {
        if (elem.getRole() == null) return "";
        String r = elem.getRole().toString();
        return r.startsWith("/") ? r.substring(1) : r;
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

    private static void readList(PdfStructElem list, StringBuilder out, Ctx ctx) {
        if (list.getKids() == null) return;
        for (IStructureNode child : list.getKids()) {
            if (!(child instanceof PdfStructElem)) continue;
            PdfStructElem e = (PdfStructElem) child;
            String r = normRole(e);
            if (StandardRoles.LI.equals(r)) {
                String text = textOf(e, ctx).trim().replaceFirst("^[•·‣\\-]\\s*", "");
                if (!text.isEmpty()) {
                    out.append("- ").append(text).append('\n');
                }
            } else if (StandardRoles.L.equals(r)) {
                readList(e, out, ctx); // 嵌套列表拍平
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
