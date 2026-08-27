package io.github.easy4j.pdf.xhtml.convert.layout;

public final class PageChunk {
    public final String text;
    public final float x, y, size;
    public final boolean bold;
    /** 等宽字体（fontName 含 mono/courier/consolas），用于代码块检测。 */
    public final boolean mono;
    public final int page, mcid;

    public PageChunk(String text, float x, float y, float size, boolean bold, boolean mono,
            int page, int mcid) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.size = size;
        this.bold = bold;
        this.mono = mono;
        this.page = page;
        this.mcid = mcid;
    }

    @Override
    public String toString() {
        return "PageChunk[" + text + "]";
    }
}
