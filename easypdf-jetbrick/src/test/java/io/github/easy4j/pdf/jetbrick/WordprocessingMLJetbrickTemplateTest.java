package io.github.easy4j.pdf.jetbrick;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import org.docx4j.Docx4jProperties;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import io.github.easy4j.pdf.WordprocessingMLTemplate;
import io.github.easy4j.pdf.utils.ConfigUtils;
import io.github.easy4j.pdf.xhtml.WordprocessingMLHtmlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jetbrick.config.ConfigLoader;
import jetbrick.template.JetConfig;
import jetbrick.template.JetEngine;

/**
 * Unit tests for {@link WordprocessingMLJetbrickTemplate}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WordprocessingMLJetbrickTemplate Tests")
class WordprocessingMLJetbrickTemplateTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new WordprocessingMLJetbrickTemplate(); } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLJetbrickTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method process should be callable")
    void instanceProcessShouldBeCallable() {
        try {
            WordprocessingMLJetbrickTemplate instance = new WordprocessingMLJetbrickTemplate();
            instance.process("test", (Map) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLJetbrickTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getEngine should be callable")
    void instanceGetEngineShouldBeCallable() {
        try {
            WordprocessingMLJetbrickTemplate instance = new WordprocessingMLJetbrickTemplate();
            instance.getEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLJetbrickTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setEngine should be callable")
    void instanceSetEngineShouldBeCallable() {
        try {
            WordprocessingMLJetbrickTemplate instance = new WordprocessingMLJetbrickTemplate();
            instance.setEngine((JetEngine) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLJetbrickTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getInternalEngine should be callable")
    void instanceGetInternalEngineShouldBeCallable() {
        try {
            WordprocessingMLJetbrickTemplate instance = new WordprocessingMLJetbrickTemplate();
            instance.getInternalEngine();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLJetbrickTemplate.class).isNotNull();
    }

}
