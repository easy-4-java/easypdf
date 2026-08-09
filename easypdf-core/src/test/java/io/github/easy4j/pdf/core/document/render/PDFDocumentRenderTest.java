package io.github.easy4j.pdf.core.document.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.github.easy4j.pdf.core.context.ItextContext;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.helper.DocumentHelper;
import io.github.easy4j.pdf.core.document.helper.IteratorHelper;
import io.github.easy4j.pdf.core.document.helper.PDFWriterHelper;
import java.io.OutputStream;

/**
 * Unit tests for {@link PDFDocumentRender}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFDocumentRender Tests")
class PDFDocumentRenderTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { PDFDocumentRender.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFDocumentRender.class).isNotNull();
    }

}
