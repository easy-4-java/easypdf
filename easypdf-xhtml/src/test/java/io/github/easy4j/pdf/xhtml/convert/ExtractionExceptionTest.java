package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

/**
 * Round 4-P2 Task 1：错误分级异常——code 携带 + IOException 兼容 + cause 透传。
 * Round5-Hotfix Task 1：追加超大文件前置拦截（LIMIT_EXCEEDED）回归用例。
 */
class ExtractionExceptionTest {

    @Test
    void carriesCodeAndIsIOException() {
        ExtractionException e = new ExtractionException(ExtractionException.Code.ENCRYPTED, "受密码保护");
        assertThat(e.getCode()).isEqualTo(ExtractionException.Code.ENCRYPTED);
        assertThat(e).isInstanceOf(java.io.IOException.class);
        assertThat(e.getMessage()).contains("密码");
    }

    @Test
    void wrapsCause() {
        RuntimeException cause = new RuntimeException("root");
        ExtractionException e = new ExtractionException(ExtractionException.Code.CORRUPT, "bad", cause);
        assertThat(e.getCause()).isSameAs(cause);
        assertThat(e.getCode()).isEqualTo(ExtractionException.Code.CORRUPT);
        assertThat(e.getMessage()).isEqualTo("bad");
    }

    // ---------------- Round5-Hotfix Task 1: maxFileBytes 前置拦截 ----------------

    @Test
    void oversizedPdfRejectedBeforeParsing(@TempDir File dir) throws Exception {
        File tiny = new File(dir, "tiny.pdf");
        Files.write(tiny.toPath(), "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
        PdfExtractionProperties props = PdfExtractionProperties.defaults();
        props.maxFileBytes = 5L; // 文件实际 > 5 字节
        assertThatThrownBy(() -> PdfStructureExtractor.extract(tiny, props))
                .isInstanceOf(ExtractionException.class)
                .extracting(e -> ((ExtractionException) e).getCode())
                .isEqualTo(ExtractionException.Code.LIMIT_EXCEEDED);
    }
}
