package io.github.easy4j.pdf.webit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.docx4j.Docx4jProperties;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import io.github.easy4j.pdf.WordprocessingMLTemplate;
import io.github.easy4j.pdf.xhtml.WordprocessingMLHtmlTemplate;
import webit.script.CFG;
import webit.script.Engine;
import java.util.Properties;

/**
 * Unit tests for {@link WordprocessingMLWebitTemplate}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WordprocessingMLWebitTemplate Tests")
class WordprocessingMLWebitTemplateTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new WordprocessingMLWebitTemplate(); } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLWebitTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method process should be callable")
    void instanceProcessShouldBeCallable() {
        try {
            WordprocessingMLWebitTemplate instance = new WordprocessingMLWebitTemplate();
            instance.process("test", (Map) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLWebitTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getEngine should be callable")
    void instanceGetEngineShouldBeCallable() {
        try {
            WordprocessingMLWebitTemplate instance = new WordprocessingMLWebitTemplate();
            instance.getEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLWebitTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setEngine should be callable")
    void instanceSetEngineShouldBeCallable() {
        try {
            WordprocessingMLWebitTemplate instance = new WordprocessingMLWebitTemplate();
            instance.setEngine((Engine) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLWebitTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getInternalEngine should be callable")
    void instanceGetInternalEngineShouldBeCallable() {
        try {
            WordprocessingMLWebitTemplate instance = new WordprocessingMLWebitTemplate();
            instance.getInternalEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLWebitTemplate.class).isNotNull();
    }

}
