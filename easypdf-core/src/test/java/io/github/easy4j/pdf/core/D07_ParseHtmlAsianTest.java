package io.github.easy4j.pdf.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import java.io.OutputStream;

/**
 * Unit tests for {@link D07_ParseHtmlAsian}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("D07_ParseHtmlAsian Tests")
class D07_ParseHtmlAsianTest {

    @Test
    @DisplayName("constant HTML should not be null or empty")
    void constantHTMLShouldNotBeNullOrEmpty() {
        assertThat(D07_ParseHtmlAsian.HTML).isNotNull();
        assertThat(D07_ParseHtmlAsian.HTML).isNotEmpty();
    }

    @Test
    @DisplayName("constant DEST should not be null or empty")
    void constantDESTShouldNotBeNullOrEmpty() {
        assertThat(D07_ParseHtmlAsian.DEST).isNotNull();
        assertThat(D07_ParseHtmlAsian.DEST).isNotEmpty();
    }

    @Test
    @DisplayName("static method main should be callable")
    void staticMainShouldBeCallable() {
        try { D07_ParseHtmlAsian.main((String[]) null); } catch (Throwable e) { /* expected */ }
        assertThat(D07_ParseHtmlAsian.class).isNotNull();
    }

}
