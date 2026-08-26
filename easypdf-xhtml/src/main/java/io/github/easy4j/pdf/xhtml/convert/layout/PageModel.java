package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.ArrayList;
import java.util.List;

public final class PageModel {
    public final int pageNo;
    public final List<PageChunk> chunks = new ArrayList<PageChunk>();
    public final List<RawImage> images = new ArrayList<RawImage>();
    public final List<RawStroke> strokes = new ArrayList<RawStroke>();

    public PageModel(int pageNo) {
        this.pageNo = pageNo;
    }
}
