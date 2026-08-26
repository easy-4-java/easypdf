package io.github.easy4j.pdf.webmvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.easy4j.pdf.PdfTemplate;
import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

class PdfTemplateViewTest {

    static class FixedTemplate extends PdfTemplate {
        @Override
        public void process(String template, java.util.Map<String, Object> variables, java.io.OutputStream out) throws Exception {
            HtmlPdfConverter.htmlToPdf("<html><body><h1>View 输出</h1></body></html>", out);
        }
    }

    @Test
    void renderProducesPdfResponse() throws Exception {
        PdfTemplateView view = new PdfTemplateView();
        view.setTemplate(new FixedTemplate());
        view.setTemplateName("tpl");

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(Collections.<String, Object>emptyMap(), new MockHttpServletRequest(), response);

        assertThat(response.getContentType()).contains("application/pdf");
        byte[] body = response.getContentAsByteArray();
        assertThat(new String(body, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
