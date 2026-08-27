package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    @Test
    void chunkSplitsByCharLimit() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "t";
        DocumentSection s = new DocumentSection();
        s.title = "一";
        s.level = 1;
        // Java 8 语法构造长文本（150 字符，等价 "第一段".repeat(50)）
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("第一段");
        }
        s.content = longText.toString();
        doc.sections = Collections.singletonList(s);
        ChunkOptions opts = new ChunkOptions();
        opts.maxChars = 100;
        opts.idPrefix = "test.pdf";
        List<DocumentChunk> chunks = DocumentChunker.chunk(doc, opts);
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.get(0).id).startsWith("test.pdf:");
        assertThat(chunks.get(0).charCount).isLessThanOrEqualTo(100);
    }

    @Test
    void chunkSingleSectionFitsOneChunk() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "t";
        DocumentSection s = new DocumentSection();
        s.title = "一";
        s.level = 1;
        s.content = "短";
        doc.sections = Collections.singletonList(s);
        ChunkOptions opts = new ChunkOptions();
        List<DocumentChunk> chunks = DocumentChunker.chunk(doc, opts);
        assertThat(chunks).hasSize(1);
    }
}
