package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.List;

public final class TableRegion {
    public final float x1, y1, x2, y2;
    public final List<Float> colXs, rowYs;

    TableRegion(float x1, float y1, float x2, float y2, List<Float> colXs, List<Float> rowYs) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        this.colXs = colXs; this.rowYs = rowYs;
    }

    public boolean contains(float px, float py) {
        return px >= x1 && px <= x2 && py >= y1 && py <= y2;
    }
}

