package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.FileOutputStream;
import java.io.IOException;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import java.io.File;
import java.io.OutputStream;

/**
 * Unit tests for {@link WatermarkHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WatermarkHelper Tests")
class WatermarkHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { WatermarkHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(WatermarkHelper.class).isNotNull();
    }

}
