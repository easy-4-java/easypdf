package io.github.easy4j.pdf.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AbstractStringTemplateWrappingPdfTemplateTest {

    static class FixedHtmlTemplate extends AbstractStringTemplateWrappingPdfTemplate {
        @Override
        protected String render(String template, Map<String, Object> variables) {
            return "<html><body><h1>" + variables.get("title") + "</h1></body></html>";
        }
    }

    @Test
    void processRendersHtmlThenPdf() throws Exception {
        FixedHtmlTemplate tpl = new FixedHtmlTemplate();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "引擎输出标题");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tpl.process("tpl", vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    static class RecordingHtmlTemplate extends AbstractStringTemplateWrappingPdfTemplate {
        boolean rendered;

        @Override
        protected String render(String template, Map<String, Object> variables) {
            this.rendered = true;
            return "<html><body><p>recorded</p></body></html>";
        }
    }

    @Test
    void renderIsCalledWithTemplate() throws Exception {
        RecordingHtmlTemplate tpl = new RecordingHtmlTemplate();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "t");
        tpl.process("mytemplate", vars, new ByteArrayOutputStream());
        assertThat(tpl.rendered).isTrue();
    }
}
