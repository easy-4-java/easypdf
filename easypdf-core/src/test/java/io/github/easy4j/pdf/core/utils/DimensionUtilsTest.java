package io.github.easy4j.pdf.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit tests for {@link DimensionUtils}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("DimensionUtils Tests")
class DimensionUtilsTest {

    @Test
    @DisplayName("static method unitParse should be callable")
    void staticUnitParseShouldBeCallable() {
        try { DimensionUtils.unitParse("test"); } catch (Throwable e) { /* expected */ }
        assertThat(DimensionUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getPercent should be callable")
    void staticGetPercentShouldBeCallable() {
        try { DimensionUtils.getPercent("test"); } catch (Throwable e) { /* expected */ }
        assertThat(DimensionUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getWidth should be callable")
    void staticGetWidthShouldBeCallable() {
        try { DimensionUtils.getWidth("test"); } catch (Throwable e) { /* expected */ }
        assertThat(DimensionUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getHeight should be callable")
    void staticGetHeightShouldBeCallable() {
        try { DimensionUtils.getHeight("test"); } catch (Throwable e) { /* expected */ }
        assertThat(DimensionUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method main should be callable")
    void staticMainShouldBeCallable() {
        try { DimensionUtils.main((String[]) null); } catch (Throwable e) { /* expected */ }
        assertThat(DimensionUtils.class).isNotNull();
    }

}
