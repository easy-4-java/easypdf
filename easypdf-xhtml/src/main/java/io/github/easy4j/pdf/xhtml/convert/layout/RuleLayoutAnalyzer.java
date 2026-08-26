package io.github.easy4j.pdf.xhtml.convert.layout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.easy4j.pdf.xhtml.convert.DocumentSection;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;
import io.github.easy4j.pdf.xhtml.convert.DocumentTable;

/**
 * 规则引擎（Tier1 格线表格 + Tier2 字号聚类/分栏/列表/页眉页脚/流式表格）。
 * 流水线（每页）：格线表格识别 → 分栏检测 → 行构建 → 页眉页脚剔除 →
 * 跨页断词合并 → 正文字号众数 → 标题判定 → 列表识别 → 流式表格 → Markdown。
 */
public final class RuleLayoutAnalyzer implements LayoutAnalyzer {

    private static final Pattern UNORDERED = Pattern.compile("^[•·◦‣\\-]\\s*");
    private static final Pattern ORDERED = Pattern.compile("^(\\d{1,2}|[a-z]|[ivxIVX]{1,4})[.)、]\\s*");
    private static final float COLUMN_GAP = 55f;
    private static final float HEAD_FACTOR = 1.22f;

    private final LatticeTableFinder tableFinder = new LatticeTableFinder();

    @Override
    public String name() {
        return "rule";
    }

    /** 行（Tier2 中间结构）：一行文本 + 最大字号 + 是否加粗。 */
    private static final class Line {
        String text;
        float size;
        boolean bold;
        float x, y;
        int page;
        List<PageChunk> chunks = new ArrayList<PageChunk>();
    }

    @Override
    public DocumentStructure analyze(List<PageModel> pages, List<int[]> taggedHeadings, String title)
            throws IOException {
        DocumentStructure doc = new DocumentStructure();
        doc.title = title;

        // 1) 每页：格线表格（含单元格图片）+ 表格外 chunks 进入分栏/行流水线
        List<Line> allLines = new ArrayList<Line>();
        List<DocumentTable> tables = new ArrayList<DocumentTable>();
        List<DocumentTable> streamTables = new ArrayList<DocumentTable>();
        List<String> looseImages = new ArrayList<String>();
        if (pages != null) {
            for (PageModel page : pages) {
                List<TableRegion> regions = tableFinder.find(page);
                List<PageChunk> flowChunks = new ArrayList<PageChunk>(page.chunks);
                for (TableRegion r : regions) {
                    DocumentTable tbl = buildTable(page, r);
                    if (tbl != null) {
                        tables.add(tbl);
                        removeInside(flowChunks, r);
                    }
                }
                for (RawImage img : page.images) {
                    boolean inTable = false;
                    for (TableRegion r : regions) {
                        if (r.contains(img.x, img.y)) { inTable = true; break; }
                    }
                    if (!inTable && img.bytes.length > 0) {
                        looseImages.add(dataUri(img));
                    }
                }
                // 分栏：x 直方图找宽空白带
                List<List<PageChunk>> columns = splitColumns(flowChunks);
                for (List<PageChunk> col : columns) {
                    allLines.addAll(buildLines(col, page.pageNo));
                }
            }
        }

        // 2) 页眉页脚剔除（≥2 页、≥60% 页面重复的顶部/底部行）
        allLines = stripHeaderFooter(allLines, pages != null ? pages.size() : 0);

        // 3) 跨页断词合并 + 4) 正文字号众数
        joinHyphenated(allLines);
        float bodySize = bodyMode(allLines);

        // 5) 组装 sections（标题切分）+ 列表 + 流式表格
        DocumentSection current = new DocumentSection();
        current.title = title != null ? title : "Document";
        current.level = 1;
        List<DocumentSection> sections = new ArrayList<DocumentSection>();
        StringBuilder body = new StringBuilder();

        int i = 0;
        while (i < allLines.size()) {
            // 流式表格尝试（连续 ≥3 行、≥2 列 x 对齐）
            int tableLen = streamTableLength(allLines, i);
            if (tableLen >= 3) {
                DocumentTable st = buildStreamTable(allLines, i, tableLen);
                streamTables.add(st);
                i += tableLen;
                continue;
            }
            Line ln = allLines.get(i);
            String text = ln.text.trim();
            if (text.isEmpty()) { i++; continue; }

            if (ln.size >= bodySize * HEAD_FACTOR && !text.isEmpty()) {
                // 标题：新 section
                if (body.length() > 0) {
                    current.content = body.toString().trim();
                    sections.add(current);
                    body = new StringBuilder();
                }
                current = new DocumentSection();
                current.title = text;
                current.level = headingLevel(allLines, i, bodySize);
                sections.add(current);
                i++;
                continue;
            }
            String lst = listMarker(text);
            if (lst != null) {
                body.append(lst).append(text.substring(markerLen(text))).append('\n');
            } else {
                body.append(text).append('\n');
            }
            i++;
        }
        current.content = body.toString().trim();
        if (!current.content.isEmpty() || current.title != null) {
            sections.add(current);
        }
        if (sections.isEmpty()) {
            current.content = "";
            sections.add(current);
        }
        doc.sections = sections;
        doc.tables.addAll(tables);
        doc.tables.addAll(streamTables);
        StringBuilder sec;
        for (String uri : looseImages) {
            doc.sections.get(doc.sections.size() - 1).content =
                (doc.sections.get(doc.sections.size() - 1).content.isEmpty() ? "" : doc.sections.get(doc.sections.size() - 1).content + "\n\n")
                + "![img](" + uri + ")";
        }
        return doc;
    }

    // ---------------- 分栏 ----------------

    private static List<List<PageChunk>> splitColumns(List<PageChunk> chunks) {
        List<List<PageChunk>> cols = new ArrayList<List<PageChunk>>();
        if (chunks.isEmpty()) return cols;
        List<PageChunk> sorted = new ArrayList<PageChunk>(chunks);
        Collections.sort(sorted, new Comparator<PageChunk>() {
            @Override public int compare(PageChunk a, PageChunk b) { return Float.compare(a.x, b.x); }
        });
        List<PageChunk> cur = new ArrayList<PageChunk>();
        float lastEnd = Float.MIN_VALUE;
        for (PageChunk c : sorted) {
            if (!cur.isEmpty()) {
                float estEnd = lastEnd + 0.0f; // 由下面重算
            }
            if (!cur.isEmpty() && c.x - endOf(cur) > COLUMN_GAP) {
                cols.add(cur);
                cur = new ArrayList<PageChunk>();
            }
            cur.add(c);
            lastEnd = c.x + c.text.length() * c.size * 0.6f;
        }
        if (!cur.isEmpty()) cols.add(cur);
        return cols;
    }

    private static float endOf(List<PageChunk> col) {
        PageChunk last = col.get(col.size() - 1);
        return last.x + last.text.length() * last.size * 0.6f;
    }

    // ---------------- 行构建 ----------------

    private static List<Line> buildLines(List<PageChunk> col, int pageNo) {
        List<Line> lines = new ArrayList<Line>();
        List<PageChunk> sorted = new ArrayList<PageChunk>(col);
        Collections.sort(sorted, new Comparator<PageChunk>() {
            @Override public int compare(PageChunk a, PageChunk b) {
                if (Math.abs(a.y - b.y) > 0.5f) return Float.compare(b.y, a.y);
                return Float.compare(a.x, b.x);
            }
        });
        Line cur = null;
        for (PageChunk c : sorted) {
            boolean sameLine = cur != null
                    && Math.abs(cur.y - c.y) <= Math.max(2f, c.size * 0.4f);
            if (!sameLine) {
                cur = new Line();
                cur.page = pageNo;
                cur.x = c.x; cur.y = c.y; cur.size = c.size; cur.bold = c.bold;
                lines.add(cur);
            }
            cur.chunks.add(c);
            if (c.size > cur.size) { cur.size = c.size; cur.bold = c.bold; }
            cur.x = Math.min(cur.x, c.x);
        }
        for (Line l : lines) {
            Collections.sort(l.chunks, new Comparator<PageChunk>() {
                @Override public int compare(PageChunk a, PageChunk b) { return Float.compare(a.x, b.x); }
            });
            StringBuilder sb = new StringBuilder();
            PageChunk prev = null;
            for (PageChunk c : l.chunks) {
                if (prev != null && sb.length() > 0) {
                    float gap = c.x - (prev.x + prev.text.length() * prev.size * 0.55f);
                    boolean latin = isLatinTail(sb) && isLatinHead(c.text);
                    if (gap > prev.size * 0.22f && latin) {
                        sb.append(' ');
                    }
                }
                sb.append(c.text);
                prev = c;
            }
            l.text = sb.toString();
        }
        return lines;
    }

    private static boolean isLatinTail(StringBuilder sb) {
        return sb.length() > 0 && sb.charAt(sb.length() - 1) < 0x2E80;
    }

    private static boolean isLatinHead(String s) {
        return s.length() > 0 && s.charAt(0) < 0x2E80;
    }

    // ---------------- 页眉页脚 ----------------

    private static List<Line> stripHeaderFooter(List<Line> lines, int pageCount) {
        if (pageCount < 2 || lines.size() < pageCount) {
            return lines;
        }
        Map<String, Integer> counts = new HashMap<String, Integer>();
        float maxY = Float.MIN_VALUE, minY = Float.MAX_VALUE;
        for (Line l : lines) {
            maxY = Math.max(maxY, l.y); minY = Math.min(minY, l.y);
        }
        for (Line l : lines) {
            boolean topZone = l.y >= maxY - 50f;
            boolean bottomZone = l.y <= minY + 50f;
            if (!topZone && !bottomZone) continue;
            String sig = Math.round(l.y / 3f) + "|" + prefix(l.text, 12);
            Integer c = counts.get(sig);
            counts.put(sig, c == null ? Integer.valueOf(1) : Integer.valueOf(c.intValue() + 1));
        }
        int threshold = (int) Math.ceil(pageCount * 0.6);
        List<Line> out = new ArrayList<Line>();
        for (Line l : lines) {
            boolean topZone = l.y >= maxY - 50f;
            boolean bottomZone = l.y <= minY + 50f;
            if (topZone || bottomZone) {
                String sig = Math.round(l.y / 3f) + "|" + prefix(l.text, 12);
                Integer c = counts.get(sig);
                if (c != null && c.intValue() >= threshold) {
                    continue; // 剔除
                }
            }
            out.add(l);
        }
        return out;
    }

    private static String prefix(String s, int n) {
        String t = s.trim();
        return t.length() <= n ? t : t.substring(0, n);
    }

    // ---------------- 断词 / 字号 / 列表 / 标题级 ----------------

    private static void joinHyphenated(List<Line> lines) {
        for (int i = 0; i < lines.size() - 1; i++) {
            String t = lines.get(i).text;
            if (t.length() > 1 && t.charAt(t.length() - 1) == '-') {
                String next = lines.get(i + 1).text;
                if (!next.isEmpty() && Character.isLetter(next.charAt(0))) {
                    lines.get(i).text = t.substring(0, t.length() - 1) + next;
                    lines.remove(i + 1);
                }
            }
        }
    }

    private static float bodyMode(List<Line> lines) {
        Map<Integer, Long> hist = new HashMap<Integer, Long>();
        for (Line l : lines) {
            int key = Math.round(l.size * 2);
            Long c = hist.get(key);
            long add = Math.max(1, l.text.length());
            hist.put(key, c == null ? add : c.longValue() + add);
        }
        long best = -1; int bestKey = 24;
        for (Map.Entry<Integer, Long> e : hist.entrySet()) {
            if (e.getValue() > best) { best = e.getValue(); bestKey = e.getKey(); }
        }
        return bestKey / 2f;
    }

    private static int headingLevel(List<Line> lines, int idx, float bodySize) {
        // 候选标题字号降序 → 1..6
        List<Float> sizes = new ArrayList<Float>();
        for (Line l : lines) {
            if (l.size >= bodySize * HEAD_FACTOR && !sizes.contains(Float.valueOf(l.size))) {
                sizes.add(l.size);
            }
        }
        Collections.sort(sizes, Collections.reverseOrder());
        int lv = sizes.indexOf(Float.valueOf(lines.get(idx).size)) + 1;
        return Math.max(1, Math.min(6, lv));
    }

    private static String listMarker(String text) {
        if (UNORDERED.matcher(text).find()) return "- ";
        if (ORDERED.matcher(text).find()) return "1. ";
        return null;
    }

    private static int markerLen(String text) {
        java.util.regex.Matcher m = UNORDERED.matcher(text);
        if (m.find()) return m.end();
        m = ORDERED.matcher(text);
        if (m.find()) return m.end();
        return 0;
    }

    // ---------------- 流式表格（无格线，x 对齐） ----------------

    private static int streamTableLength(List<Line> lines, int start) {
        if (start >= lines.size()) {
            return 0;
        }
        List<Float> first = clusterStarts(lines.get(start));
        if (first.size() < 2) {
            return 0;
        }
        int n = 1;
        for (int i = start + 1; i < lines.size(); i++) {
            List<Float> cs = clusterStarts(lines.get(i));
            if (cs.size() != first.size() || !aligned(first, cs)) {
                break;
            }
            n++;
        }
        return n;
    }

    /** 行内列簇起始 x（列边界：净间隙 > max(size*1.2, 12pt)）。 */
    private static List<Float> clusterStarts(Line l) {
        List<Float> xs = new ArrayList<Float>();
        PageChunk prev = null;
        for (PageChunk c : l.chunks) {
            if (prev == null || c.x - (prev.x + prev.text.length() * prev.size * 0.55f) > Math.max(prev.size * 1.2f, 12f)) {
                xs.add(Float.valueOf(c.x));
            }
            prev = c;
        }
        return xs;
    }

    /** 各行第 k 列起始 x 跨行对齐（±6pt）。 */
    private static boolean aligned(List<Float> a, List<Float> b) {
        for (int i = 0; i < a.size(); i++) {
            if (Math.abs(a.get(i).floatValue() - b.get(i).floatValue()) > 6f) {
                return false;
            }
        }
        return true;
    }

    private static DocumentTable buildStreamTable(List<Line> lines, int start, int len) {
        DocumentTable tbl = new DocumentTable();
        for (int i = 0; i < len; i++) {
            Line l = lines.get(start + i);
            List<String> cells = new ArrayList<String>();
            StringBuilder cell = new StringBuilder();
            PageChunk prev = null;
            for (PageChunk c : l.chunks) {
                if (prev != null && c.x - (prev.x + prev.text.length() * prev.size * 0.55f) > Math.max(prev.size * 1.2f, 12f)) {
                    cells.add(cell.toString().trim());
                    cell = new StringBuilder();
                }
                cell.append(c.text);
                prev = c;
            }
            cells.add(cell.toString().trim());
            if (i == 0) {
                tbl.headers.add(cells);
            } else {
                tbl.rows.add(cells);
            }
        }
        return tbl;
    }

    // ---------------- Tier1 lattice（沿用） ----------------

    private static DocumentTable buildTable(PageModel page, TableRegion r) {
        DocumentTable tbl = new DocumentTable();
        int nCols = r.colXs.size() - 1;
        int nRows = r.rowYs.size() - 1;
        if (nCols < 1 || nRows < 1) {
            return null;
        }
        for (int row = nRows - 1; row >= 0; row--) {
            float top = r.rowYs.get(row + 1).floatValue();
            float bottom = r.rowYs.get(row).floatValue();
            List<String> cells = new ArrayList<String>();
            for (int col = 0; col < nCols; col++) {
                float left = r.colXs.get(col).floatValue();
                float right = r.colXs.get(col + 1).floatValue();
                cells.add(cellContent(page, left, right, bottom, top));
            }
            if (row == nRows - 1) {
                tbl.headers.add(cells);
            } else {
                tbl.rows.add(cells);
            }
        }
        return tbl;
    }

    private static String cellContent(PageModel page, float left, float right, float bottom, float top) {
        List<PageChunk> inCell = new ArrayList<PageChunk>();
        for (PageChunk c : page.chunks) {
            if (c.x >= left && c.x < right && c.y > bottom && c.y <= top) {
                inCell.add(c);
            }
        }
        Collections.sort(inCell, new Comparator<PageChunk>() {
            @Override public int compare(PageChunk a, PageChunk b) { return Float.compare(a.x, b.x); }
        });
        StringBuilder sb = new StringBuilder();
        for (PageChunk c : inCell) {
            sb.append(c.text.trim());
        }
        for (RawImage img : page.images) {
            if (img.x >= left && img.x < right && img.y > bottom && img.y <= top && img.bytes.length > 0) {
                sb.append(" ![img](").append(dataUri(img)).append(')');
            }
        }
        return sb.toString().trim();
    }

    private static String dataUri(RawImage img) {
        String ext = img.ext == null ? "png" : img.ext.toLowerCase();
        String mime = "image/png";
        if (ext.contains("jpg") || ext.contains("jpeg")) mime = "image/jpeg";
        else if (ext.contains("gif")) mime = "image/gif";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(img.bytes);
    }

    private static void removeInside(List<PageChunk> chunks, TableRegion r) {
        for (int i = chunks.size() - 1; i >= 0; i--) {
            PageChunk c = chunks.get(i);
            if (r.contains(c.x, c.y)) {
                chunks.remove(i);
            }
        }
    }
}
