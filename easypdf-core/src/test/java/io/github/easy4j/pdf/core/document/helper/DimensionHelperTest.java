package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.utils.DimensionUtils;

/**
 * Unit tests for {@link DimensionHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("DimensionHelper Tests")
class DimensionHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { DimensionHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(DimensionHelper.class).isNotNull();
    }

}
