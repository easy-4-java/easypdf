package io.github.easy4j.pdf.xhtml.convert.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

class PageModelListenerTest {

    private static PdfDocument render(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        return new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void collectsTextChunksWithSizeAndPosition() throws Exception {
        try (PdfDocument doc = render("<html><body><h1>标题</h1><p>正文段落</p></body></html>")) {
            List<PageModel> models = PageModelListener.collect(doc);
            assertThat(models).hasSize(1);
            assertThat(models.get(0).chunks.toString()).contains("标题").contains("正文段落");
            PageChunk h = findChunk(models.get(0), "标题");
            PageChunk p = findChunk(models.get(0), "正文");
            assertThat(h.size).isGreaterThan(p.size);
        }
    }

    @Test
    void collectsImagesWithBytesAndPosition() throws Exception {
        String png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
        try (PdfDocument doc = render("<html><body><p><img src=\"" + png + "\"/></p></body></html>")) {
            List<PageModel> models = PageModelListener.collect(doc);
            assertThat(models.get(0).images).isNotEmpty();
            assertThat(models.get(0).images.get(0).bytes.length).isGreaterThan(0);
        }
    }

    @Test
    void collectsRulingStrokesFromBorderedTable() throws Exception {
        try (PdfDocument doc = render(
                "<html><body><table border='1'><tr><td>A</td><td>B</td></tr></table></body></html>")) {
            List<PageModel> models = PageModelListener.collect(doc);
            assertThat(models.get(0).strokes.size()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void taggedChunksCarryMcid() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        io.github.easy4j.pdf.core.convert.HtmlPdfConverter.htmlToPdfTagged(
                "<html><body><h1>标题</h1><p>正文</p></body></html>", out);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
            List<PageModel> models = PageModelListener.collect(doc);
            boolean anyMcid = false;
            for (PageChunk c : models.get(0).chunks) {
                if (c.mcid >= 0) { anyMcid = true; break; }
            }
            assertThat(anyMcid).isTrue();
        }
    }

    private static PageChunk findChunk(PageModel m, String contains) {
        for (PageChunk c : m.chunks) {
            if (c.text.contains(contains)) return c;
        }
        throw new AssertionError("chunk not found: " + contains);
    }
}
