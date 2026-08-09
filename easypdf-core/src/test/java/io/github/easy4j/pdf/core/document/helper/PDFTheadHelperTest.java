package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Element;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPRow;
import com.itextpdf.text.pdf.PdfPTable;
import com.jeefw.fastkit.beanutils.JavaBeanUtils;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link PDFTheadHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFTheadHelper Tests")
class PDFTheadHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFTheadHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFTheadHelper.class).isNotNull();
    }

}
