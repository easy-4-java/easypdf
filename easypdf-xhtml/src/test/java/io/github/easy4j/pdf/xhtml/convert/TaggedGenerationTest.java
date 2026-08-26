package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

class TaggedGenerationTest {

    @Test
    void markdownToPdfTaggedProducesStructTree() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyPdf.markdownToPdfTagged("# 标题一\n\n正文\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n- 项目", out);
        byte[] bytes = out.toByteArray();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            assertThat(doc.getStructTreeRoot()).isNotNull();
            assertThat(doc.isTagged()).isTrue();
        }
    }
}
