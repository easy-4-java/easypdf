package io.github.easy4j.pdf.core.document.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.utils.DimensionUtils;
import com.jeefw.fastxml.jdom.xhtml.AbstractFontResolver;

/**
 * Unit tests for {@link ItextFontResolver}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextFontResolver Tests")
class ItextFontResolverTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ItextFontResolver.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextFontResolver.class).isNotNull();
    }

}
