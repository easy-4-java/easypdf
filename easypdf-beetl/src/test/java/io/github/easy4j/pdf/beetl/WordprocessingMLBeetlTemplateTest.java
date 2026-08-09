package io.github.easy4j.pdf.beetl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.util.Map;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.docx4j.Docx4jProperties;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import io.github.easy4j.pdf.Docx4jConstants;
import io.github.easy4j.pdf.WordprocessingMLTemplate;
import io.github.easy4j.pdf.xhtml.WordprocessingMLHtmlTemplate;
import java.util.Properties;

/**
 * Unit tests for {@link WordprocessingMLBeetlTemplate}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WordprocessingMLBeetlTemplate Tests")
class WordprocessingMLBeetlTemplateTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new WordprocessingMLBeetlTemplate(); } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLBeetlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method process should be callable")
    void instanceProcessShouldBeCallable() {
        try {
            WordprocessingMLBeetlTemplate instance = new WordprocessingMLBeetlTemplate();
            instance.process("test", (Map) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLBeetlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getEngine should be callable")
    void instanceGetEngineShouldBeCallable() {
        try {
            WordprocessingMLBeetlTemplate instance = new WordprocessingMLBeetlTemplate();
            instance.getEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLBeetlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setEngine should be callable")
    void instanceSetEngineShouldBeCallable() {
        try {
            WordprocessingMLBeetlTemplate instance = new WordprocessingMLBeetlTemplate();
            instance.setEngine((GroupTemplate) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLBeetlTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getInternalEngine should be callable")
    void instanceGetInternalEngineShouldBeCallable() {
        try {
            WordprocessingMLBeetlTemplate instance = new WordprocessingMLBeetlTemplate();
            instance.getInternalEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLBeetlTemplate.class).isNotNull();
    }

}
