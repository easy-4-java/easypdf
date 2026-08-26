package io.github.easy4j.pdf.xhtml.convert.layout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.easy4j.pdf.xhtml.convert.DocumentSection;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;
import io.github.easy4j.pdf.xhtml.convert.DocumentTable;

/**
 * 规则引擎（Tier1 格线表格 + Tier2 字号聚类/分栏/列表）。
 * 流水线：每页先识别格线表格（含单元格内图片 base64），剩余文本进入正文流（Tier2 逐步充实）。
 */
public final class RuleLayoutAnalyzer implements LayoutAnalyzer {

    private final LatticeTableFinder tableFinder = new LatticeTableFinder();

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
        StringBuilder flow = new StringBuilder();

        if (pages != null) {
            for (PageModel page : pages) {
                List<TableRegion> regions = tableFinder.find(page);
                // 表格区域内的 chunks/images 不再进入正文流
                List<PageChunk> flowChunks = new ArrayList<PageChunk>(page.chunks);
                for (TableRegion r : regions) {
                    DocumentTable tbl = buildTable(page, r);
                    if (tbl != null) {
                        doc.tables.add(tbl);
                        removeInside(flowChunks, r);
                    }
                }
                // 剩余文本按 (y 降序, x 升序) 排序进入正文流
                Collections.sort(flowChunks, new Comparator<PageChunk>() {
                    @Override
                    public int compare(PageChunk a, PageChunk b) {
                        if (a.y != b.y) return Float.compare(b.y, a.y);
                        return Float.compare(a.x, b.x);
                    }
                });
                for (PageChunk c : flowChunks) {
                    flow.append(c.text);
                }
                if (flowChunks.size() > 0 && flow.length() > 0) {
                    flow.append('\n').append('\n');
                }
                // 表格外散落的独立图片
                for (RawImage img : page.images) {
                    boolean inTable = false;
                    for (TableRegion r : regions) {
                        if (r.contains(img.x, img.y)) { inTable = true; break; }
                    }
                    if (!inTable && img.bytes.length > 0) {
                        flow.append("![").append("img").append("](").append(dataUri(img)).append(")\n\n");
                    }
                }
            }
        }
        sec.content = flow.toString().trim();
        doc.sections.add(sec);
        return doc;
    }

    /** 网格 → DocumentTable：cell 文本（x 排序拼接）+ 单元格内图片 base64。 */
    private static DocumentTable buildTable(PageModel page, TableRegion r) {
        DocumentTable tbl = new DocumentTable();
        int nCols = r.colXs.size() - 1;
        int nRows = r.rowYs.size() - 1;
        if (nCols < 1 || nRows < 1) {
            return null;
        }
        // rowYs 升序（PDF y 向上），视觉自上而下 = 反向遍历
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
            @Override
            public int compare(PageChunk a, PageChunk b) {
                return Float.compare(a.x, b.x);
            }
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
