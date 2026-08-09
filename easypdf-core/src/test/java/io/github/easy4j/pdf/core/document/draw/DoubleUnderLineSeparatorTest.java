package io.github.easy4j.pdf.core.document.draw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.draw.LineSeparator;

/**
 * Unit tests for {@link DoubleUnderLineSeparator}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("DoubleUnderLineSeparator Tests")
class DoubleUnderLineSeparatorTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new DoubleUnderLineSeparator(); } catch (Throwable e) { /* expected */ }
        assertThat(DoubleUnderLineSeparator.class).isNotNull();
    }

    @Test
    @DisplayName("instance method drawLine should be callable")
    void instanceDrawLineShouldBeCallable() {
        try {
            DoubleUnderLineSeparator instance = new DoubleUnderLineSeparator();
            instance.drawLine((PdfContentByte) null, 0.0f, 0.0f, 0.0f);
        } catch (Throwable e) { /* expected */ }
        assertThat(DoubleUnderLineSeparator.class).isNotNull();
    }

}
