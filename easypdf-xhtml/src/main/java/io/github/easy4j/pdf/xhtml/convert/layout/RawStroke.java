package io.github.easy4j.pdf.xhtml.convert.layout;

public final class RawStroke {
    public final float x1, y1, x2, y2, width;
    public final int page;

    public RawStroke(float x1, float y1, float x2, float y2, float width, int page) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.width = width;
        this.page = page;
    }

    public boolean horizontal() {
        return Math.abs(y1 - y2) < 0.5f && Math.abs(x1 - x2) > 1f;
    }

    public boolean vertical() {
        return Math.abs(x1 - x2) < 0.5f && Math.abs(y1 - y2) > 1f;
    }
}
