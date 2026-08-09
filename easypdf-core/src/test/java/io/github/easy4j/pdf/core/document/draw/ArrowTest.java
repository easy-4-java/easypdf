package io.github.easy4j.pdf.core.document.draw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.pdf.draw.VerticalPositionMark;

/**
 * Unit tests for {@link Arrow}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("Arrow Tests")
class ArrowTest {

    @Test
    @DisplayName("class should be loadable")
    void classShouldBeLoadable() {
        assertThat(Arrow.class.getName()).isEqualTo("io.github.easy4j.pdf.core.document.draw.Arrow");
    }

}
