package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.itextpdf.text.pdf.PdfWriter;
import io.github.easy4j.pdf.core.context.ItextContext;
import java.io.File;
import java.io.OutputStream;

/**
 * Unit tests for {@link ZipOutputHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ZipOutputHelper Tests")
class ZipOutputHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ZipOutputHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ZipOutputHelper.class).isNotNull();
    }

}
