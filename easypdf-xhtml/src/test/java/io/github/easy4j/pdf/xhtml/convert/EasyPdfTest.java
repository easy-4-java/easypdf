package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EasyPdfTest {

    @TempDir
    File tempDir;

    @Test
    void markdownToPdfProducesPdf() throws Exception {
        File out = new File(tempDir, "doc.pdf");
        String md = "# 合同\n\n甲方：张三\n\n| 项目 | 金额 |\n|---|---|\n| 服务 | 100 |";
        EasyPdf.markdownToPdf(md, out);

        byte[] bytes = Files.readAllBytes(out.toPath());
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void pdfToMarkdownExtractsText() throws Exception {
        File out = new File(tempDir, "roundtrip.pdf");
        EasyPdf.markdownToPdf("# 标题\n\n正文 hello", out);

        String md = EasyPdf.pdfToMarkdown(out);
        assertThat(md).contains("标题").contains("hello");
    }

    @Test
    void pdfToMarkdownAcceptsInputStream() throws Exception {
        File out = new File(tempDir, "stream.pdf");
        EasyPdf.markdownToPdf("流式输入测试", out);

        try (FileInputStream in = new FileInputStream(out)) {
            String md = EasyPdf.pdfToMarkdown(in);
            assertThat(md).contains("流式输入测试");
        }
    }
}
