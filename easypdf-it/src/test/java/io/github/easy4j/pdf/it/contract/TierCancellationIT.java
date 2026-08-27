package io.github.easy4j.pdf.it.contract;

import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;
import io.github.easy4j.pdf.xhtml.convert.PdfStructureExtractor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cancellation-token contract: extractPerPage must honor a PageConsumer returning
 * {@code false} to abort further pages.
 *
 * <p><b>Status:</b> requires a multi-page PDF fixture (≥ 5 pages) which doesn't yet
 * exist in {@code src/test/resources/contracts/}. Disabled until the fixture lands.
 */
class TierCancellationIT {

    @Test
    @Disabled("enable after multi-page-cancellable.pdf fixture is committed")
    @DisplayName("extractPerPage: returning false aborts after current page")
    void cancellationHaltsExtraction() throws Exception {
        File pdf = fixture("multi-page-cancellable.pdf");
        AtomicInteger visited = new AtomicInteger();

        PdfStructureExtractor.extractPerPage(pdf, null, (pageNo, part) -> {
            visited.incrementAndGet();
            // 第三页就停
            return pageNo < 3;
        });

        // 实际回调页数：1, 2, 3（pageNo<3 时继续 → 1, 2 都返回 true；pageNo=3 返回 false）
        assertThat(visited.get()).isEqualTo(3);
        assertThat(visited.get()).isLessThan(totalPages(pdf));
    }

    private static File fixture(String name) {
        URL url = TierCancellationIT.class.getResource("/contracts/" + name);
        assertThat((Object) url).as("fixture %s missing", name).isNotNull();
        return Paths.get(url.getPath()).toFile();
    }

    private static int totalPages(File pdf) throws Exception {
        try (com.itextpdf.kernel.pdf.PdfReader r = new com.itextpdf.kernel.pdf.PdfReader(pdf);
             com.itextpdf.kernel.pdf.PdfDocument d = new com.itextpdf.kernel.pdf.PdfDocument(r)) {
            return d.getNumberOfPages();
        }
    }

    @SuppressWarnings("unused")
    private static DocumentStructure ignore(DocumentStructure d) {
        return d;
    }
}