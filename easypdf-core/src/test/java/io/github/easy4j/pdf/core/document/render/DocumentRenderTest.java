package io.github.easy4j.pdf.core.document.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link DocumentRender}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("DocumentRender Tests")
class DocumentRenderTest {

    @Test
    @DisplayName("should be abstract")
    void shouldBeAbstract() {
        assertThat(DocumentRender.class).isAbstract();
    }

    @Test
    @DisplayName("class should be loadable")
    void classShouldBeLoadable() {
        assertThat(DocumentRender.class.getName()).isEqualTo("io.github.easy4j.pdf.core.document.render.DocumentRender");
    }

}
