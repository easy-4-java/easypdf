package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Round 4-P2 Task 1：错误分级异常——code 携带 + IOException 兼容 + cause 透传。
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
}
