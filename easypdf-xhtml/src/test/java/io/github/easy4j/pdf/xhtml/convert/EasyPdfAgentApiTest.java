package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

class EasyPdfAgentApiTest {

    @Test
    void summaryExposesSectionTree() throws Exception {
        File pdf = writeSimplePdf();
        DocumentSummary sum = EasyPdf.summary(pdf);
        assertThat(sum.title).isNotEmpty();
        assertThat(sum.totalPages).isPositive();
    }

    @Test
    void pageRangeReturnsMarkdownSubset() throws Exception {
        File pdf = writeSimplePdf();
        String md = EasyPdf.pageRange(pdf, 1, 1);
        assertThat(md).isNotBlank();
    }

    @Test
    void chunkedProducesRagReadyChunks() throws Exception {
        File pdf = writeSimplePdf();
        ChunkOptions opts = new ChunkOptions();
        opts.maxChars = 200;
        List<DocumentChunk> chunks = EasyPdf.chunked(pdf, opts);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).id).isNotNull();
    }

    private File writeSimplePdf() throws Exception {
        String html = "<html><body>"
            + "<h1>报告标题</h1><p>第一章正文内容，用于验证门面聚合入口。</p>"
            + "</body></html>";
        File f = File.createTempFile("easypdf-agent-", ".pdf");
        f.deleteOnExit();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        Files.write(f.toPath(), out.toByteArray());
        return f;
    }
}
