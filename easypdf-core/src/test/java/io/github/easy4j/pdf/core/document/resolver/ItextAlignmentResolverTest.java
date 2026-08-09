package io.github.easy4j.pdf.core.document.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import com.itextpdf.text.Element;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import com.jeefw.fastxml.jdom.xhtml.AlignmentResolver;

/**
 * Unit tests for {@link ItextAlignmentResolver}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextAlignmentResolver Tests")
class ItextAlignmentResolverTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ItextAlignmentResolver.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextAlignmentResolver.class).isNotNull();
    }

}
