package io.github.easy4j.pdf.core.context.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ConfigConstants}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ConfigConstants Tests")
class ConfigConstantsTest {

    @Test
    @DisplayName("class should be loadable")
    void classShouldBeLoadable() {
        assertThat(ConfigConstants.class.getName()).isEqualTo("io.github.easy4j.pdf.core.context.constants.ConfigConstants");
    }

}
