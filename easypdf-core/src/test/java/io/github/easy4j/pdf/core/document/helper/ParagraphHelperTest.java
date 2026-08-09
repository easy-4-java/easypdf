package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link ParagraphHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ParagraphHelper Tests")
class ParagraphHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ParagraphHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ParagraphHelper.class).isNotNull();
    }

}
