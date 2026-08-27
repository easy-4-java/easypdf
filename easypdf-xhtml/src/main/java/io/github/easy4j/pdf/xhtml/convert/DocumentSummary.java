package io.github.easy4j.pdf.xhtml.convert;

import java.util.ArrayList;
import java.util.List;

public final class DocumentSummary {
    public String title;
    public int totalPages;
    public int totalChars;
    public int totalTables;
    public int totalImages;
    public List<DocumentSummarySection> sections = new ArrayList<DocumentSummarySection>();
}
