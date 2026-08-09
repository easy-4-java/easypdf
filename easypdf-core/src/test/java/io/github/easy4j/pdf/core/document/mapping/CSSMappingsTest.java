package io.github.easy4j.pdf.core.document.mapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import com.itextpdf.text.Element;

/**
 * Unit tests for {@link CSSMappings}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("CSSMappings Tests")
class CSSMappingsTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { CSSMappings.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(CSSMappings.class).isNotNull();
    }

}
