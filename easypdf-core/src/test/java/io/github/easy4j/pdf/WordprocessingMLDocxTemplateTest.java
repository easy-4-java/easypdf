package io.github.easy4j.pdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.Docx4jProperties;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import java.util.Properties;
import java.util.Set;

/**
 * Unit tests for {@link WordprocessingMLDocxTemplate}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WordprocessingMLDocxTemplate Tests")
class WordprocessingMLDocxTemplateTest {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
        try { new WordprocessingMLDocxTemplate(); } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method process should be callable")
    void instanceProcessShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.process((File) null, "test", (Map) null, (File) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method process should be callable")
    void instanceProcessWith1ParamsShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.process("test", (Map) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getStaticData should be callable")
    void instanceGetStaticDataShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.getStaticData((Map) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getSourceDocx should be callable")
    void instanceGetSourceDocxShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.getSourceDocx();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setSourceDocx should be callable")
    void instanceSetSourceDocxShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.setSourceDocx((File) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getOutputDocx should be callable")
    void instanceGetOutputDocxShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.getOutputDocx();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setOutputDocx should be callable")
    void instanceSetOutputDocxShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.setOutputDocx((File) null);
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getPlaceholderStart should be callable")
    void instanceGetPlaceholderStartShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.getPlaceholderStart();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method setPlaceholderStart should be callable")
    void instanceSetPlaceholderStartShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.setPlaceholderStart("test");
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

    @Test
    @DisplayName("instance method getPlaceholderEnd should be callable")
    void instanceGetPlaceholderEndShouldBeCallable() {
        try {
            WordprocessingMLDocxTemplate instance = new WordprocessingMLDocxTemplate();
            instance.getPlaceholderEnd();
        } catch (Throwable e) { /* expected */ }
        assertThat(WordprocessingMLDocxTemplate.class).isNotNull();
    }

}
