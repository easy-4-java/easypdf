package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfToMarkdownConverterTest {

    @TempDir
    File tempDir;

    @Test
    void pdfToStructuredRejectsNullFile() {
        assertThatThrownBy(() -> PdfToMarkdownConverter.pdfToStructured((File) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void pdfToStructuredFromMissingFileThrowsIOException() {
        assertThatThrownBy(() -> PdfToMarkdownConverter.pdfToStructured((File) new File("/no.pdf")))
                .isInstanceOf(IOException.class);
    }

    @Test
    void easyPdfFacadeExposesStructuredMethod() throws Exception {
        // 伪 PDF 字节（iText 严格校验会抛 RuntimeException/IOException 之一）—— 接受任一异常
        File fake = new File(tempDir, "fake.pdf");
        Files.write(fake.toPath(), "%PDF-1.4\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            EasyPdf.pdfToStructuredMarkdown(fake);
            throw new AssertionError("expected exception");
        } catch (Exception expected) {
            // IOException 或 iText 的 BadPdfException（RuntimeException）均视为契约
        }
    }
}
