package io.github.easy4j.pdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 * Unit tests for {@link WordprocessingMLTemplate}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WordprocessingMLTemplate Tests")
class WordprocessingMLTemplateTest {

    @Test
    @DisplayName("should be abstract")
    void shouldBeAbstract() {
        assertThat(WordprocessingMLTemplate.class).isAbstract();
    }

    @Test
    @DisplayName("class should be loadable")
    void classShouldBeLoadable() {
        assertThat(WordprocessingMLTemplate.class.getName()).isEqualTo("io.github.easy4j.pdf.WordprocessingMLTemplate");
    }

}
