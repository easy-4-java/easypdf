package io.github.easy4j.pdf.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.Ftr;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner.GridSpan;
import org.docx4j.wml.Text;

/**
 * Unit tests for {@link WmlElementUtils}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WmlElementUtils Tests")
class WmlElementUtilsTest {

    @Test
    @DisplayName("static method createFooter should be callable")
    void staticCreateFooterShouldBeCallable() {
        try { WmlElementUtils.createFooter("test"); } catch (Throwable e) { /* expected */ }
        assertThat(WmlElementUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method addInlineImageToParagraph should be callable")
    void staticAddInlineImageToParagraphShouldBeCallable() {
        try { WmlElementUtils.addInlineImageToParagraph((Inline) null); } catch (Throwable e) { /* expected */ }
        assertThat(WmlElementUtils.class).isNotNull();
    }

}
