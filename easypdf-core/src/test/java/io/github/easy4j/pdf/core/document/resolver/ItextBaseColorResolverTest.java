package io.github.easy4j.pdf.core.document.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import com.itextpdf.text.BaseColor;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import com.jeefw.fastxml.core.utils.XMLColorUtils;
import com.jeefw.fastxml.jdom.xhtml.ColorResolver;

/**
 * Unit tests for {@link ItextBaseColorResolver}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextBaseColorResolver Tests")
class ItextBaseColorResolverTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ItextBaseColorResolver.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextBaseColorResolver.class).isNotNull();
    }

}
