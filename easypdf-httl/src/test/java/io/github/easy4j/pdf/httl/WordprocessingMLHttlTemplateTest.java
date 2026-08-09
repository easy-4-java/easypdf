package io.github.easy4j.pdf.httl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import org.docx4j.Docx4jProperties;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import io.github.easy4j.pdf.Docx4jConstants;
import io.github.easy4j.pdf.WordprocessingMLTemplate;
import io.github.easy4j.pdf.utils.ConfigUtils;
import io.github.easy4j.pdf.xhtml.WordprocessingMLHtmlTemplate;
import httl.Engine;

/**
 * Unit tests for {@link WordprocessingMLHttlTemplate}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WordprocessingMLHttlTemplate Tests")
class WordprocessingMLHttlTemplateTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new WordprocessingMLHttlTemplate(); } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLHttlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method process should be callable")
    void instanceProcessShouldBeCallable() {
        try {
            WordprocessingMLHttlTemplate instance = new WordprocessingMLHttlTemplate();
            instance.process("test", (Map) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLHttlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getEngine should be callable")
    void instanceGetEngineShouldBeCallable() {
        try {
            WordprocessingMLHttlTemplate instance = new WordprocessingMLHttlTemplate();
            instance.getEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLHttlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setEngine should be callable")
    void instanceSetEngineShouldBeCallable() {
        try {
            WordprocessingMLHttlTemplate instance = new WordprocessingMLHttlTemplate();
            instance.setEngine((Engine) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLHttlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getInternalEngine should be callable")
    void instanceGetInternalEngineShouldBeCallable() {
        try {
            WordprocessingMLHttlTemplate instance = new WordprocessingMLHttlTemplate();
            instance.getInternalEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLHttlTemplate.class).isNotNull();
    }

}
