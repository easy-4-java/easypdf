package io.github.easy4j.pdf.xhtml.convert;

import java.util.ArrayList;
import java.util.List;

public final class DocumentSection {
    public String title;
    public int level;
    public String content = "";
    public List<DocumentSection> children = new ArrayList<DocumentSection>();
    public List<DocumentTable> tables = new ArrayList<DocumentTable>();
    public List<DocumentImage> images = new ArrayList<DocumentImage>();
}
