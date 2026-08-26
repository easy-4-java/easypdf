package io.github.easy4j.pdf.xhtml.convert.layout;

public final class RawImage {
    public final byte[] bytes;
    public final String ext;
    public final float x, y, w, h;
    public final int page, mcid;

    public RawImage(byte[] bytes, String ext, float x, float y, float w, float h, int page, int mcid) {
        this.bytes = bytes;
        this.ext = ext;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.page = page;
        this.mcid = mcid;
    }
}
