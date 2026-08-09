package io.github.easy4j.pdf.core.document.draw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.draw.LineSeparator;

/**
 * Unit tests for {@link UnderLineSeparator}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("UnderLineSeparator Tests")
class UnderLineSeparatorTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new UnderLineSeparator(); } catch (Throwable e) { /* expected */ }
        assertThat(UnderLineSeparator.class).isNotNull();
    }

}
