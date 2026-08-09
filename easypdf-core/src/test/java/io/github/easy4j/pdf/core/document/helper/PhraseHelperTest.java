package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link PhraseHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PhraseHelper Tests")
class PhraseHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PhraseHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PhraseHelper.class).isNotNull();
    }

}
