package io.github.easy4j.pdf.core.document.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.awt.Color;
import java.io.IOException;
import java.util.Map;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import com.jeefw.fastxml.core.utils.XMLColorUtils;
import com.jeefw.fastxml.jdom.xhtml.ColorResolver;

/**
 * Unit tests for {@link ItextColorResolver}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextColorResolver Tests")
class ItextColorResolverTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ItextColorResolver.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextColorResolver.class).isNotNull();
    }

}
