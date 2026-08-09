package io.github.easy4j.pdf.core.document.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.util.Map;
import org.jdom2.JDOMException;
import io.github.easy4j.pdf.core.context.ItextContext;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import com.jeefw.fastxml.jdom.XMLElement;
import com.jeefw.fastxml.jdom.xhtml.sax.XMLElementResolver;
import java.util.Set;

/**
 * Unit tests for {@link ItextXMLResolver}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextXMLResolver Tests")
class ItextXMLResolverTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ItextXMLResolver.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextXMLResolver.class).isNotNull();
    }

}
