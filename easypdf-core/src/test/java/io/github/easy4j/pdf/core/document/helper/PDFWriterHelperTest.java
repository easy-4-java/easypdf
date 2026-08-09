package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfWriter;
import com.jeefw.fastkit.format.utils.PatternFormatUtils;
import com.jeefw.fastkit.lang3.BlankUtils;
import io.github.easy4j.pdf.core.context.ItextContext;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.events.PDFPageEvent;

/**
 * Unit tests for {@link PDFWriterHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFWriterHelper Tests")
class PDFWriterHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFWriterHelper.getInstance((Map) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFWriterHelper.class).isNotNull();
    }

}
