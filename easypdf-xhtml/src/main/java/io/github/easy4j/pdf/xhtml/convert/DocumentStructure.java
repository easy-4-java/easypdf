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
            appendSections(sb, sections);
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
        boolean dedup = title != null && !title.isEmpty()
                && sections != null && !sections.isEmpty()
                && sections.get(0).level == 1
                && title.trim().equals(sections.get(0).title == null ? "" : sections.get(0).title.trim());
        if (!dedup && title != null && !title.isEmpty()) {
            sb.append("# ").append(title).append('\n').append('\n');
        }
        sb.append(toMarkdown());
        return sb.toString();
    }

    /**
     * 序列化一组同级 sections：跳过与前一个已输出 section 标题相同且无任何内容的空段
     * （相邻重复去重；从"只查 title vs sections[0]"扩展到任意相邻重复）。
     * 仅对 content/children/tables/images 全空的条目去重，携带内容的同名段不丢弃。
     */
    private static void appendSections(StringBuilder sb, List<DocumentSection> secs) {
        if (secs == null) return;
        String lastTitle = null;
        boolean any = false;
        for (DocumentSection s : secs) {
            if (any && isEmptyEntry(s) && normTitle(s.title).equals(lastTitle)) {
                continue; // 相邻重复空段：只输出一次
            }
            appendSection(sb, s);
            lastTitle = normTitle(s.title);
            any = true;
        }
    }

    private static String normTitle(String t) {
        return t == null ? "" : t.trim();
    }

    private static boolean isEmptyEntry(DocumentSection s) {
        return (s.content == null || s.content.isEmpty())
                && (s.children == null || s.children.isEmpty())
                && (s.tables == null || s.tables.isEmpty())
                && (s.images == null || s.images.isEmpty());
    }

    private static void appendSection(StringBuilder sb, DocumentSection s) {
        for (int i = 0; i < s.level; i++) {
            sb.append('#');
        }
        sb.append(' ').append(s.title == null ? "" : s.title).append('\n').append('\n');
        if (s.content != null && !s.content.isEmpty()) {
            sb.append(s.content.trim()).append('\n').append('\n');
        }
        appendSections(sb, s.children);
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
                sb.append('|').append(joinCells(row)).append('\n');
            }
            return;
        }
        for (List<String> hdr : t.headers) {
            sb.append('|').append(joinCells(hdr)).append('\n');
        }
        sb.append('|');
        for (int i = 0; i < t.headers.get(0).size(); i++) {
            sb.append(" --- |");
        }
        sb.append('\n');
        for (List<String> row : t.rows) {
            sb.append('|').append(joinCells(row)).append('\n');
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
