package io.github.easy4j.pdf.xhtml.convert;

import java.util.ArrayList;
import java.util.List;

public final class DocumentStructure {
    public String title;
    public List<DocumentSection> sections = new ArrayList<DocumentSection>();
    public List<DocumentTable> tables = new ArrayList<DocumentTable>();
    public List<DocumentImage> images = new ArrayList<DocumentImage>();

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        if (sections != null) {
            for (DocumentSection s : sections) {
                appendSection(sb, s);
            }
        }
        if (tables != null) {
            for (DocumentTable t : tables) {
                appendTable(sb, t);
            }
        }
        if (images != null) {
            for (DocumentImage i : images) {
                appendImage(sb, i);
            }
        }
        return sb.toString().trim();
    }

    public String fullMarkdown() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append("# ").append(title).append('\n').append('\n');
        }
        sb.append(toMarkdown());
        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, DocumentSection s) {
        for (int i = 0; i < s.level; i++) {
            sb.append('#');
        }
        sb.append(' ').append(s.title == null ? "" : s.title).append('\n').append('\n');
        if (s.content != null && !s.content.isEmpty()) {
            sb.append(s.content.trim()).append('\n').append('\n');
        }
        for (DocumentSection c : s.children) {
            appendSection(sb, c);
        }
        for (DocumentTable t : s.tables) {
            appendTable(sb, t);
        }
        for (DocumentImage i : s.images) {
            appendImage(sb, i);
        }
    }

    private static void appendTable(StringBuilder sb, DocumentTable t) {
        if (t.headers == null || t.headers.isEmpty()) {
            for (List<String> row : t.rows) {
                sb.append('|').append(joinCells(row)).append("|\n");
            }
            return;
        }
        for (List<String> hdr : t.headers) {
            sb.append('|').append(joinCells(hdr)).append("|\n");
        }
        sb.append('|');
        for (int i = 0; i < t.headers.get(0).size(); i++) {
            sb.append(" --- |");
        }
        sb.append('\n');
        for (List<String> row : t.rows) {
            sb.append('|').append(joinCells(row)).append("|\n");
        }
        sb.append('\n');
    }

    private static String joinCells(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (String c : cells) {
            sb.append(' ').append(c == null ? "" : c.trim()).append(" |");
        }
        return sb.toString();
    }

    private static void appendImage(StringBuilder sb, DocumentImage i) {
        sb.append("![").append(i.alt == null ? "" : i.alt).append(']')
          .append('(').append(i.src == null ? "" : i.src).append(")\n\n");
    }
}
