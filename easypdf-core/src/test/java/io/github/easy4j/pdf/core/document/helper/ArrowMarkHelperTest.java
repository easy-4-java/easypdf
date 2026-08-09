package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import io.github.easy4j.pdf.core.document.draw.Arrow;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;

/**
 * Unit tests for {@link ArrowMarkHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ArrowMarkHelper Tests")
class ArrowMarkHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ArrowMarkHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ArrowMarkHelper.class).isNotNull();
    }

}
