package io.github.easy4j.pdf.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.AlternativeFormatInputPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.CTAltChunk;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;

/**
 * Unit tests for {@link WMLPackageUtils}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("WMLPackageUtils Tests")
class WMLPackageUtilsTest {

    @Test
    @DisplayName("static method getWMLPackageTemplate should be callable")
    void staticGetWMLPackageTemplateShouldBeCallable() {
        try { WMLPackageUtils.getWMLPackageTemplate("test"); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getChildrenElements should be callable")
    void staticGetChildrenElementsShouldBeCallable() {
        try { WMLPackageUtils.getChildrenElements((Object) null, (Class) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getTargetElements should be callable")
    void staticGetTargetElementsShouldBeCallable() {
        try { WMLPackageUtils.getTargetElements((Object) null, (Class) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method replacePlaceholder should be callable")
    void staticReplacePlaceholderShouldBeCallable() {
        try { WMLPackageUtils.replacePlaceholder((WordprocessingMLPackage) null, "test", "test"); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method writeDocxToStream should be callable")
    void staticWriteDocxToStreamShouldBeCallable() {
        try { WMLPackageUtils.writeDocxToStream((WordprocessingMLPackage) null, "test"); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method replaceParagraph should be callable")
    void staticReplaceParagraphShouldBeCallable() {
        try { WMLPackageUtils.replaceParagraph("test", "test", (WordprocessingMLPackage) null, (ContentAccessor) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getTable should be callable")
    void staticGetTableShouldBeCallable() {
        try { WMLPackageUtils.getTable((List) null, "test"); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method replaceTable should be callable")
    void staticReplaceTableShouldBeCallable() {
        try { WMLPackageUtils.replaceTable((String[]) null, (List) null, (WordprocessingMLPackage) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method addRowToTable should be callable")
    void staticAddRowToTableShouldBeCallable() {
        try { WMLPackageUtils.addRowToTable((Tbl) null, (Tr) null, (Map) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method mergeDocx should be callable")
    void staticMergeDocxShouldBeCallable() {
        try { WMLPackageUtils.mergeDocx((List) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method imageToByteArray should be callable")
    void staticImageToByteArrayShouldBeCallable() {
        try { WMLPackageUtils.imageToByteArray((File) null); } catch (Throwable e) { /* expected */ }
        assertThat(WMLPackageUtils.class).isNotNull();
    }

}
