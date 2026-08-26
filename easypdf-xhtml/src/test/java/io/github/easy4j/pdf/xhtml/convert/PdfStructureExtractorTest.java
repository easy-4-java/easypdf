package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;

import org.junit.jupiter.api.Test;

class PdfStructureExtractorTest {

    @Test
    void extractRejectsNullFile() {
        assertThatThrownBy(() -> PdfStructureExtractor.extract(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void extractRejectsMissingFile() {
        assertThatThrownBy(() -> PdfStructureExtractor.extract(new File("/nonexistent.pdf")))
                .isInstanceOf(java.io.IOException.class);
    }
}
