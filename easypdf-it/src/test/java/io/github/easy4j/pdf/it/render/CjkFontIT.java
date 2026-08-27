package io.github.easy4j.pdf.it.render;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CJK font fallback: rendering HTML containing CJK characters must not lose text.
 *
 * <p><b>Status:</b> {@link Disabled @Disabled} until the system is known to carry
 * the required fonts and a fixture template is committed. See
 * {@code docs/MAINTENANCE.md} for the font installation list.
 */
class CjkFontIT {

    @Test
    @Disabled("enable after font fixture + Chinese template are committed")
    @DisplayName("CJK: 中文标题 / 正文 渲染后 PDF 中文本可被抽取")
    void cjkRenderingRoundTrip() {
    }
}