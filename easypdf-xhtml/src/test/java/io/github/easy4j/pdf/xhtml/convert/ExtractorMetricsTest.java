package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExtractorMetricsTest {

    @Test
    void countsSuccessesAndFailures() {
        ExtractorMetrics m = new ExtractorMetrics();
        m.recordSuccess(100);
        m.recordSuccess(200);
        m.recordFailure(ExtractionException.Code.NOT_FOUND, 50);
        m.recordFailure(ExtractionException.Code.LIMIT_EXCEEDED, 0);
        java.util.Map<String, Long> snap = m.snapshot();
        assertThat(snap.get("total")).isEqualTo(4L);
        assertThat(snap.get("failures.NOT_FOUND")).isEqualTo(1L);
        assertThat(snap.get("failures.LIMIT_EXCEEDED")).isEqualTo(1L);
        assertThat(snap.get("durationMs")).isEqualTo(350L);
        assertThat(snap.get("failures")).isNull(); // 顶层失败 key 不存在
    }

    @Test
    void snapshotIsIsolated() {
        ExtractorMetrics m = new ExtractorMetrics();
        m.recordSuccess(10);
        java.util.Map<String, Long> snap = m.snapshot();
        m.recordSuccess(20); // 修改后再 snapshot
        java.util.Map<String, Long> snap2 = m.snapshot();
        assertThat(snap.get("total")).isEqualTo(1L); // 快照不变
        assertThat(snap2.get("total")).isEqualTo(2L);
    }

    @Test
    void resetClearsAll() {
        ExtractorMetrics m = new ExtractorMetrics();
        m.recordSuccess(10);
        m.reset();
        assertThat(m.snapshot().get("total")).isZero();
    }
}
