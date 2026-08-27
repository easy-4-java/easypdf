package io.github.easy4j.pdf.it.contract;

import io.github.easy4j.pdf.xhtml.convert.ExtractReport;
import io.github.easy4j.pdf.xhtml.convert.ExtractionException;
import io.github.easy4j.pdf.xhtml.convert.ExtractorMetrics;
import io.github.easy4j.pdf.xhtml.convert.PdfStructureExtractor;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract suite: PDF → Markdown deterministic output for hand-picked fixture PDFs.
 *
 * <p>Fixtures live under {@code src/test/resources/contracts/} and snapshots under
 * {@code src/test/resources/snapshots/}. Snapshots MUST be hand-reviewed; the
 * purpose of this suite is to catch <em>unintended</em> drift in extractor output,
 * not to silently rubber-stamp it.
 *
 * <p><b>Status:</b> snapshot-driven cases are {@link Disabled @Disabled} until the
 * fixtures (deterministic PDFs) and expected markdown snapshots are committed by
 * hand. Non-fixture negative cases run on every {@code mvn verify}.
 */
class MarkdownContractIT {

    private static final String SNAPSHOTS = "/snapshots";
    private static final String CONTRACTS = "/contracts";

    private static File fixture(String name) {
        URL url = MarkdownContractIT.class.getResource(CONTRACTS + "/" + name);
        assertThat((Object) url).as("fixture %s missing — commit it under src/test/resources/contracts/", name)
                .isNotNull();
        return Paths.get(url.getPath()).toFile();
    }

    @Test
    @Disabled("enable after single-page-plain.pdf fixture + snapshot are committed")
    @DisplayName("contract: single-page plain text")
    void singlePagePlainText() throws Exception {
        String md = PdfStructureExtractor.extract(fixture("single-page-plain.pdf"))
                .toMarkdown();
        assertThat(md).isEqualTo(readSnapshot("single-page-plain"));
    }

    @Test
    @Disabled("enable after multi-page-table.pdf fixture + snapshot are committed")
    @DisplayName("contract: multi-page with table")
    void multiPageWithTable() throws Exception {
        String md = PdfStructureExtractor.extract(fixture("multi-page-table.pdf"))
                .toMarkdown();
        assertThat(md).isEqualTo(readSnapshot("multi-page-table"));
    }

    @Test
    @Disabled("enable after tagged-report.pdf fixture + snapshot are committed")
    @DisplayName("contract: tagged PDF round-trip")
    void taggedRoundTrip() throws Exception {
        String md = PdfStructureExtractor.extract(fixture("tagged-report.pdf"))
                .toMarkdown();
        assertThat(md).isEqualTo(readSnapshot("tagged-report"));
    }

    @Test
    @Disabled("enable after cjk-mixed.pdf fixture + snapshot are committed")
    @DisplayName("contract: cjk font fallback")
    void cjkFallback() throws Exception {
        String md = PdfStructureExtractor.extract(fixture("cjk-mixed.pdf"))
                .toMarkdown();
        // CJK 字形在不同 iText 渲染下可能微变；只断言保留中文段存在
        assertThat(md).contains("标题").contains("正文");
    }

    @Test
    @Disabled("enable after single-page-plain.pdf fixture is committed")
    @DisplayName("contract: extractWithReport counts success in metrics")
    void extractWithReportPopulatesMetrics() throws Exception {
        // INSTANCE 是进程级共享，单测之间可能互有污染；
        // 只断言"调用后 successes 计数严格 +1"。
        long successBefore = ExtractorMetrics.INSTANCE.snapshot()
                .getOrDefault("successes", 0L);

        ExtractReport report = PdfStructureExtractor.extractWithReport(
                fixture("single-page-plain.pdf"),
                PdfExtractionProperties.defaults());

        assertThat(report).isNotNull();
        assertThat(report.document).isNotNull();
        long successAfter = ExtractorMetrics.INSTANCE.snapshot()
                .getOrDefault("successes", 0L);
        assertThat(successAfter - successBefore).isEqualTo(1L);
    }

    @Test
    @DisplayName("contract: NOT_FOUND surfaces as ExtractionException")
    void missingFileSurfacesAsNotFound() {
        File missing = new File("target/does-not-exist.pdf");
        assertThatThrownBy(() -> PdfStructureExtractor.extract(missing))
                .isInstanceOf(ExtractionException.class)
                .extracting(e -> ((ExtractionException) e).getCode())
                .isEqualTo(ExtractionException.Code.NOT_FOUND);
    }

    @Test
    @Disabled("enable after encrypted.pdf fixture is committed")
    @DisplayName("contract: ENCRYPTED PDF is classified")
    void encryptedPdfIsClassified() {
        // 由 RobustnessTest 等单测覆盖生成加密 PDF；IT 这里只断言默认 props 行为
        // ——遇到 ENCRYPTED 抛 ExtractionException(Code.ENCRYPTED)
        File encrypted = fixture("encrypted.pdf");
        assertThatThrownBy(() -> PdfStructureExtractor.extract(encrypted))
                .isInstanceOf(ExtractionException.class)
                .extracting(e -> ((ExtractionException) e).getCode())
                .isEqualTo(ExtractionException.Code.ENCRYPTED);
    }

    private static String readSnapshot(String name) throws Exception {
        URL url = MarkdownContractIT.class.getResource(SNAPSHOTS + "/" + name + ".md");
        assertThat((Object) url).as("snapshot %s.md missing", name).isNotNull();
        return new String(java.nio.file.Files.readAllBytes(Paths.get(url.getPath())), "UTF-8");
    }
}