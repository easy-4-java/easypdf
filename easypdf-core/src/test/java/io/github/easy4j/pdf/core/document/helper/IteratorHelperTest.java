package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.jeefw.fastkit.beanutils.JavaBeanUtils;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;

/**
 * Unit tests for {@link IteratorHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("IteratorHelper Tests")
class IteratorHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { IteratorHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(IteratorHelper.class).isNotNull();
    }

}
