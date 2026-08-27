package io.github.easy4j.pdf.it.webmvc;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import io.github.easy4j.pdf.webmvc.AbstractITextPdfView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end Spring MVC view layer test: render an {@link AbstractITextPdfView}
 * via mocked servlet API and assert the response carries a valid PDF byte stream.
 *
 * <p><b>3.0.x branch only:</b> uses {@code jakarta.servlet} imports; 2.0.x and
 * 1.0.x branches use {@code javax.servlet} and would need a separate
 * jakarta-vs-javax fork of this class — not currently maintained on those
 * branches.
 */
class WebMvcPdfViewIT {

    @Test
    @DisplayName("AbstractITextPdfView: renders valid PDF into HTTP response")
    void abstractViewRendersValidPdf() throws Exception {
        HelloPdfView view = new HelloPdfView();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        view.render(new HashMap<String, Object>(), request, response);

        assertThat(response.getContentType()).isEqualTo("application/pdf");
        byte[] body = response.getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // %PDF- 文件头
        assertThat(new String(body, 0, 5, "UTF-8")).isEqualTo("%PDF-");

        // 字节流确实是合法 PDF：reader 能打开，且至少有 1 页
        try (java.io.ByteArrayInputStream bis =
                     new java.io.ByteArrayInputStream(response.getContentAsByteArray());
             PdfReader reader = new PdfReader(bis);
             PdfDocument doc = new PdfDocument(reader)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    static class HelloPdfView extends AbstractITextPdfView {
        @Override
        protected void buildPdfDocument(Map<String, Object> model, Document document,
                                        PdfDocument pdfDocument, HttpServletRequest request,
                                        HttpServletResponse response) {
            document.add(new Paragraph("hello itext webmvc"));
        }
    }
}