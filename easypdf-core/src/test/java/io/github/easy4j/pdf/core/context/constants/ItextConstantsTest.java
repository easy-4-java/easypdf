package io.github.easy4j.pdf.core.context.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.File;

/**
 * Unit tests for {@link ItextConstants}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextConstants Tests")
class ItextConstantsTest {

    @Test
    @DisplayName("constant XSD_NAME should not be null or empty")
    void constantXSD_NAMEShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.XSD_NAME).isNotNull();
        assertThat(ItextConstants.XSD_NAME).isNotEmpty();
    }

    @Test
    @DisplayName("constant NOT_FOUNT_FILE_ON_PATH should not be null or empty")
    void constantNOT_FOUNT_FILE_ON_PATHShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.NOT_FOUNT_FILE_ON_PATH).isNotNull();
        assertThat(ItextConstants.NOT_FOUNT_FILE_ON_PATH).isNotEmpty();
    }

    @Test
    @DisplayName("constant CORE20016 should not be null or empty")
    void constantCORE20016ShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.CORE20016).isNotNull();
        assertThat(ItextConstants.CORE20016).isNotEmpty();
    }

    @Test
    @DisplayName("constant LOGXS_FILE_NAME should not be null or empty")
    void constantLOGXS_FILE_NAMEShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.LOGXS_FILE_NAME).isNotNull();
        assertThat(ItextConstants.LOGXS_FILE_NAME).isNotEmpty();
    }

    @Test
    @DisplayName("constant PATH_EXCEPTION_LOGX should not be null or empty")
    void constantPATH_EXCEPTION_LOGXShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.PATH_EXCEPTION_LOGX).isNotNull();
        assertThat(ItextConstants.PATH_EXCEPTION_LOGX).isNotEmpty();
    }

    @Test
    @DisplayName("constant KEY_CONFIG_FILE_PATH should not be null or empty")
    void constantKEY_CONFIG_FILE_PATHShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.KEY_CONFIG_FILE_PATH).isNotNull();
        assertThat(ItextConstants.KEY_CONFIG_FILE_PATH).isNotEmpty();
    }

    @Test
    @DisplayName("constant KEY_CONF_IMAGESERVLET_PATH should not be null or empty")
    void constantKEY_CONF_IMAGESERVLET_PATHShouldNotBeNullOrEmpty() {
        assertThat(ItextConstants.KEY_CONF_IMAGESERVLET_PATH).isNotNull();
        assertThat(ItextConstants.KEY_CONF_IMAGESERVLET_PATH).isNotEmpty();
    }

}
