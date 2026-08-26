package io.github.easy4j.pdf.velocity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VelocityPdfTemplateTest {

    @TempDir
    File tempDir;

    @Test
    void rendersVelocityTemplateToPdf() throws Exception {
        File tpl = new File(tempDir, "invoice.vm");
        Files.write(tpl.toPath(), "<html><body><h1>测试标题 ${title}</h1></body></html>".getBytes(StandardCharsets.UTF_8));

        Properties ps = new Properties();
        ps.setProperty("resource.loader", "file");
        ps.setProperty("file.resource.loader.path", tempDir.getAbsolutePath());
        ps.setProperty("input.encoding", "UTF-8");
        ps.setProperty("output.encoding", "UTF-8");
        VelocityEngine engine = new VelocityEngine(ps);

        VelocityPdfTemplate template = new VelocityPdfTemplate();
        template.setEngine(engine);
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "INV-001");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        template.process("invoice.vm", vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
