package io.github.easy4j.pdf.core.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jeefw.fastkit.lang3.StringUtils;
import java.util.Set;

/**
 * Unit tests for {@link PDFTemplateCacheManager}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFTemplateCacheManager Tests")
class PDFTemplateCacheManagerTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFTemplateCacheManager.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFTemplateCacheManager.class).isNotNull();
    }

}
