package io.github.easy4j.pdf.core.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlPdfConverterTest {

    @TempDir
    File tempDir;

    @Test
    void htmlToPdfProducesPdfWithChineseText() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = "<html><body><h1>中文合同标题</h1><p>正文段落 hello</p></body></html>";
        HtmlPdfConverter.htmlToPdf(html, out);

        byte[] bytes = out.toByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void pdfToTextExtractsChineseAndLatin() throws Exception {
        File pdf = new File(tempDir, "golden.pdf");
        String html = "<html><body><h1>发票 Invoice</h1><p>金额 amount 128.00</p></body></html>";
        HtmlPdfConverter.htmlToPdf(html, Files.newOutputStream(pdf.toPath()));

        String text = HtmlPdfConverter.pdfToText(pdf);
        assertThat(text).contains("发票").contains("Invoice").contains("128.00");
    }
}
