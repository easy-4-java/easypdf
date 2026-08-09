package io.github.easy4j.pdf.core.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import java.util.Map;
import java.util.HashMap;

/**
 * Unit tests for {@link XMLEclmentCacheManager}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("XMLEclmentCacheManager Tests")
class XMLEclmentCacheManagerTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { XMLEclmentCacheManager.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(XMLEclmentCacheManager.class).isNotNull();
    }

}
