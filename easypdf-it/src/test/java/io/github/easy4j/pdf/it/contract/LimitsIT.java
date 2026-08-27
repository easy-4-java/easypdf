package io.github.easy4j.pdf.it.contract;

import io.github.easy4j.pdf.xhtml.convert.ExtractionException;
import io.github.easy4j.pdf.xhtml.convert.PdfStructureExtractor;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Limits contract: PdfExtractionProperties.maxFileBytes / maxPages guard must trip
 * LIMIT_EXCEEDED before the parser opens the document.
 *
 * <p><b>Status:</b> requires real multi-page fixture PDFs of known size. Disabled
 * until fixtures land.
 */
class LimitsIT {

    @Test
    @Disabled("enable after a fixture with known size is committed")
    @DisplayName("maxFileBytes: oversized file trips LIMIT_EXCEEDED before read")
    void maxFileBytesTripsBeforeRead() throws Exception {
        File pdf = fixture("any-valid.pdf");
        long actual = Files.size(Paths.get(pdf.toURI()));

        PdfExtractionProperties tight = PdfExtractionProperties.defaults();
        tight.maxFileBytes = actual - 1; // 故意比实际小 1 字节

        assertThatThrownBy(() -> PdfStructureExtractor.extract(pdf, tight))
                .isInstanceOf(ExtractionException.class)
                .extracting(e -> ((ExtractionException) e).getCode())
                .isEqualTo(ExtractionException.Code.LIMIT_EXCEEDED);
    }

    @Test
    @Disabled("enable after multi-page-with-known-count.pdf fixture is committed")
    @DisplayName("maxPages: oversized page count trips LIMIT_EXCEEDED on open")
    void maxPagesTripsOnOpen() throws Exception {
        File pdf = fixture("multi-page-with-known-count.pdf");

        PdfExtractionProperties tight = PdfExtractionProperties.defaults();
        tight.maxPages = 1;

        assertThatThrownBy(() -> PdfStructureExtractor.extract(pdf, tight))
                .isInstanceOf(ExtractionException.class)
                .extracting(e -> ((ExtractionException) e).getCode())
                .isEqualTo(ExtractionException.Code.LIMIT_EXCEEDED);
    }

    private static File fixture(String name) {
        URL url = LimitsIT.class.getResource("/contracts/" + name);
        assertThat((Object) url).as("fixture %s missing", name).isNotNull();
        return Paths.get(url.getPath()).toFile();
    }
}