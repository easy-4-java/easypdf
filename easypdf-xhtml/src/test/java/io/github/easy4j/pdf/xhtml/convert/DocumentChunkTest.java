package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentChunkTest {

    @Test
    void chunkHoldsContentAndMetadata() {
        DocumentChunk c = new DocumentChunk();
        c.id = "abc.pdf:1-2:0";
        c.source = "abc.pdf";
        c.title = "合同";
        c.pageStart = 1;
        c.pageEnd = 2;
        c.level = 1;
        c.text = "这是第一段内容。";
        c.charCount = 9;
        assertThat(c.id).contains("abc.pdf").contains("1-2");
        assertThat(c.charCount).isEqualTo(9);
    }

    @Test
    void chunkEmptyDefaultsAreZero() {
        DocumentChunk c = new DocumentChunk();
        assertThat(c.id).isNull();
        assertThat(c.charCount).isZero();
    }
}
