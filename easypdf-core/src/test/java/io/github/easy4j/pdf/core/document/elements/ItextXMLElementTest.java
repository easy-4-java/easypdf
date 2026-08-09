package io.github.easy4j.pdf.core.document.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.awt.Color;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import io.github.easy4j.pdf.core.document.resolver.ItextAlignmentResolver;
import io.github.easy4j.pdf.core.document.resolver.ItextColorResolver;
import io.github.easy4j.pdf.core.document.resolver.ItextFontResolver;

/**
 * Unit tests for {@link ItextXMLElement}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextXMLElement Tests")
class ItextXMLElementTest {

    @Test
    @DisplayName("should be abstract")
    void shouldBeAbstract() {
        assertThat(ItextXMLElement.class).isAbstract();
    }

}
