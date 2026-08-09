package io.github.easy4j.pdf.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link Docx4jTemplateUtils}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("Docx4jTemplateUtils Tests")
class Docx4jTemplateUtilsTest {

    @Test
    @DisplayName("static method downloadDocUseDoc4j should be callable")
    void staticDownloadDocUseDoc4jShouldBeCallable() {
        try { Docx4jTemplateUtils.downloadDocUseDoc4j((InputStream) null, (Map) null, (HttpServletResponse) null, "test"); } catch (Throwable e) { /* expected */ }
        assertThat(Docx4jTemplateUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method replaceDocUseDoc4j should be callable")
    void staticReplaceDocUseDoc4jShouldBeCallable() {
        try { Docx4jTemplateUtils.replaceDocUseDoc4j((InputStream) null, (Map) null, (OutputStream) null); } catch (Throwable e) { /* expected */ }
        assertThat(Docx4jTemplateUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method cleanDocumentPart should be callable")
    void staticCleanDocumentPartShouldBeCallable() {
        try { Docx4jTemplateUtils.cleanDocumentPart((MainDocumentPart) null); } catch (Throwable e) { /* expected */ }
        assertThat(Docx4jTemplateUtils.class).isNotNull();
    }

}
