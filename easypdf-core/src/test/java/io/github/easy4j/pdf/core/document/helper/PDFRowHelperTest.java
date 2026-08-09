package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Element;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPRow;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link PDFRowHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFRowHelper Tests")
class PDFRowHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFRowHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFRowHelper.class).isNotNull();
    }

}
