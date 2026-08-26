package io.github.easy4j.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PdfTemplateTest {

    /** 最小实现：把变量渲染进 HTML 后交给 HtmlPdfConverter。 */
    static class HtmlPdfTemplateImpl extends PdfTemplate {
        @Override
        public void process(String template, Map<String, Object> variables, OutputStream out) throws Exception {
            String html = "<html><body><h1>" + variables.get("title") + "</h1></body></html>";
            io.github.easy4j.pdf.core.convert.HtmlPdfConverter.htmlToPdf(html, out);
        }
    }

    @Test
    void processWritesPdfToOutputStream() throws Exception {
        PdfTemplate template = new HtmlPdfTemplateImpl();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "合同 Contract");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        template.process("tpl", vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void processConvenienceMethodReturnsByteArray() throws Exception {
        PdfTemplate template = new HtmlPdfTemplateImpl();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "报告 Report");

        ByteArrayOutputStream out = template.process("tpl", vars);
        assertThat(out.toByteArray()).isNotEmpty();
    }
}
