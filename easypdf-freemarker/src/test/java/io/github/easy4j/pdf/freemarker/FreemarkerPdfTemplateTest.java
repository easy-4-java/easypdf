package io.github.easy4j.pdf.freemarker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import freemarker.cache.FileTemplateLoader;

class FreemarkerPdfTemplateTest {

    @TempDir
    File tempDir;

    @Test
    void rendersFreemarkerTemplateToPdf() throws Exception {
        File tpl = new File(tempDir, "invoice.ftl");
        Files.write(tpl.toPath(), "<html><body><h1>发票 ${no}</h1></body></html>".getBytes(StandardCharsets.UTF_8));

        FreemarkerPdfTemplate template = new FreemarkerPdfTemplate();
        template.setDefaultEncoding("UTF-8");
        template.setPreTemplateLoaders(new FileTemplateLoader(tempDir));
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("no", "INV-001");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        template.process("invoice.ftl", vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
