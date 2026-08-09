package io.github.easy4j.pdf.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link BaseFontFactory}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("BaseFontFactory Tests")
class BaseFontFactoryTest {

    @Test
    @DisplayName("class should be loadable")
    void classShouldBeLoadable() {
        assertThat(BaseFontFactory.class.getName()).isEqualTo("io.github.easy4j.pdf.core.BaseFontFactory");
    }

}
