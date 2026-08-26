package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 格线表格区域：聚类后的行线 y 序列与列线 x 序列（升序）+ 外包盒。
 */
/**
 * Tier1 格线（Lattice）表格检测：水平/垂直笔画按坐标聚类为行线/列线，
 * 行≥2 且列≥2 视为表格网格（Tabula lattice 思想的 iText7 实现）。
 */
public final class LatticeTableFinder {

    private static final float CLUSTER_TOL = 2.5f;

    public List<TableRegion> find(PageModel page) {
        List<TableRegion> regions = new ArrayList<TableRegion>();
        if (page.strokes.size() < 4) {
            return regions;
        }
        List<Float> rowYs = cluster(positions(page, true));
        List<Float> colXs = cluster(positions(page, false));
        if (rowYs.size() < 2 || colXs.size() < 2) {
            return regions;
        }
        // 行线与列线需实际相交（x 覆盖列跨度 / y 覆盖行跨度）才认定同一网格
        float rx1 = maxStart(page, true), rx2 = minEnd(page, true);
        float ry1 = maxStart(page, false), ry2 = minEnd(page, false);
        if (rx1 >= rx2 || ry1 >= ry2) {
            return regions;
        }
        regions.add(new TableRegion(rx1, ry1, rx2, ry2, colXs, rowYs));
        return regions;
    }

    private static List<Float> positions(PageModel page, boolean horizontal) {
        List<Float> out = new ArrayList<Float>();
        for (RawStroke s : page.strokes) {
            if (horizontal ? s.horizontal() : s.vertical()) {
                out.add(horizontal ? (s.y1 + s.y2) / 2f : (s.x1 + s.x2) / 2f);
            }
        }
        return out;
    }

    /** 行线段的最大左端 / 列线段的最大下端（求网格外包盒）。 */
    private static float maxStart(PageModel page, boolean horizontal) {
        float v = Float.MAX_VALUE;
        for (RawStroke s : page.strokes) {
            if (horizontal ? s.horizontal() : s.vertical()) {
                float st = horizontal ? Math.min(s.x1, s.x2) : Math.min(s.y1, s.y2);
                v = Math.min(v, st);
            }
        }
        return v;
    }

    private static float minEnd(PageModel page, boolean horizontal) {
        float v = -Float.MAX_VALUE;
        for (RawStroke s : page.strokes) {
            if (horizontal ? s.horizontal() : s.vertical()) {
                float en = horizontal ? Math.max(s.x1, s.x2) : Math.max(s.y1, s.y2);
                v = Math.max(v, en);
            }
        }
        return v;
    }

    /** 坐标聚类（差 < CLUSTER_TOL 合并），返回升序去重位置。 */
    private static List<Float> cluster(List<Float> raw) {
        List<Float> sorted = new ArrayList<Float>(raw);
        Collections.sort(sorted);
        List<Float> out = new ArrayList<Float>();
        for (float v : sorted) {
            if (out.isEmpty() || v - out.get(out.size() - 1) > CLUSTER_TOL) {
                out.add(Float.valueOf(v));
            }
        }
        return out;
    }
}
