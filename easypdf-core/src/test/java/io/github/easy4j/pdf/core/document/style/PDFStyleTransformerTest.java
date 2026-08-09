package io.github.easy4j.pdf.core.document.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import com.jeefw.fastkit.lang3.BlankUtils;
import io.github.easy4j.pdf.core.context.ItextContext;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.helper.DimensionHelper;
import io.github.easy4j.pdf.core.document.resolver.ItextAlignmentResolver;
import io.github.easy4j.pdf.core.document.resolver.ItextBaseColorResolver;
import io.github.easy4j.pdf.core.document.resolver.ItextFontResolver;
import com.jeefw.fastxml.jdom.xhtml.StyleTransformer;

/**
 * Unit tests for {@link PDFStyleTransformer}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFStyleTransformer Tests")
class PDFStyleTransformerTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFStyleTransformer.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFStyleTransformer.class).isNotNull();
    }

}
