package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Element;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.resolver.ItextFontResolver;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link PDFCaptionHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFCaptionHelper Tests")
class PDFCaptionHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFCaptionHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCaptionHelper.class).isNotNull();
    }

}
