package io.github.easy4j.pdf.xhtml.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agent API：把 {@link DocumentStructure} 按字符数切片为 {@link DocumentChunk} 流
 * （RAG / Embedding 友好）。
 *
 * <p>算法：把每个 section 的标题+正文拼为文本流，跨页硬切（不做页内合并，
 * 避免丢失 {@link DocumentSection#page} 页码锚点）；相邻 chunk 重叠
 * {@code overlapChars} 字符；需继续切分时优先在段尾（空行）处下刀，保住段落完整性。
 * 表格/图片不参与切片（后续版本再拍平进段落流）。
 */
public final class DocumentChunker {

    private DocumentChunker() {
    }

    public static List<DocumentChunk> chunk(DocumentStructure doc, ChunkOptions opts) {
        Objects.requireNonNull(doc, "doc must not be null");
        ChunkOptions o = opts != null ? opts : new ChunkOptions();
        String prefix = o.idPrefix == null || o.idPrefix.isEmpty() ? "doc" : o.idPrefix;
        int max = Math.max(o.maxChars, 1);
        int overlap = Math.min(Math.max(o.overlapChars, 0), max - 1);
        List<DocumentChunk> out = new ArrayList<DocumentChunk>();
        if (doc.sections != null) {
            for (DocumentSection sec : doc.sections) {
                chunkSection(prefix, sec, max, overlap, out);
            }
        }
        return out;
    }

    private static void chunkSection(String prefix, DocumentSection s, int max, int overlap,
            List<DocumentChunk> out) {
        StringBuilder text = new StringBuilder();
        if (s.title != null && !s.title.isEmpty()) {
            text.append(s.title).append("\n\n");
        }
        if (s.content != null) {
            text.append(s.content);
        }
        String combined = text.toString();
        if (combined.isEmpty()) {
            return;
        }
        // 未超限整节单发，不在标题后的空行处误开新片
        if (combined.length() <= max) {
            out.add(newChunk(prefix, s, combined, 0, combined.length()));
            return;
        }
        int idx = 0;
        while (idx < combined.length()) {
            int end = Math.min(idx + max, combined.length());
            int cut = end;
            if (end < combined.length()) {
                // 还需继续切时才找段尾（\n\n）边界，且边界必须推进（> idx）
                int brk = combined.lastIndexOf("\n\n", end);
                if (brk > idx) {
                    cut = brk;
                }
            }
            out.add(newChunk(prefix, s, combined, idx, cut));
            if (cut >= combined.length()) {
                break;
            }
            // 重叠 overlap 字符续切（至少前进 1 字符保证收敛）
            idx = Math.max(cut - overlap, idx + 1);
        }
    }

    private static DocumentChunk newChunk(String prefix, DocumentSection s,
            String combined, int start, int cut) {
        DocumentChunk c = new DocumentChunk();
        c.id = prefix + ":" + start + "-" + cut;
        c.source = prefix;
        c.title = s.title;
        c.pageStart = s.page;
        c.pageEnd = s.page;
        c.level = s.level;
        c.text = combined.substring(start, cut);
        c.charCount = c.text.length();
        return c;
    }
}
