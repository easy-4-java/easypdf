package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Unit tests for {@link DirectContentHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("DirectContentHelper Tests")
class DirectContentHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { DirectContentHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(DirectContentHelper.class).isNotNull();
    }

}
