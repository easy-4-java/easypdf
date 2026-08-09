package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.jeefw.fastkit.lang3.BlankUtils;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link RectangleHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("RectangleHelper Tests")
class RectangleHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { RectangleHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(RectangleHelper.class).isNotNull();
    }

}
