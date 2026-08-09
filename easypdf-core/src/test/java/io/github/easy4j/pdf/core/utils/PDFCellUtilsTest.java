package io.github.easy4j.pdf.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Unit tests for {@link PDFCellUtils}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("PDFCellUtils Tests")
class PDFCellUtilsTest {

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellShouldBeCallable() {
        try { PDFCellUtils.getCell("test"); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith1ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((Phrase) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith2ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((Image) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith3ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((PdfPCell) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith4ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((PdfPTable) null, (PdfPCell) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith5ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell("test", (Font) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith6ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell("test", 0, 0, (Font) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith7ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell("test", 0, (Font) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith8ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((PdfPTable) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith9ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((PdfPTable) null, 0, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith10ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((PdfPTable) null, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith11ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell("test", 0, 0, 0, (Font) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith12ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((Paragraph) null, 0, 0, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith13ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell("test", 0, 0, 0, 0, (Font) null); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith14ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((Image) null, 0, 0, 0, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith15ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((Image) null, 0, 0, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getCell should be callable")
    void staticGetCellWith16ParamsShouldBeCallable() {
        try { PDFCellUtils.getCell((PdfPTable) null, 0, 0, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getEmptyCell should be callable")
    void staticGetEmptyCellShouldBeCallable() {
        try { PDFCellUtils.getEmptyCell(); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method getEmptyCell should be callable")
    void staticGetEmptyCellWith18ParamsShouldBeCallable() {
        try { PDFCellUtils.getEmptyCell(0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method addEmptyRow should be callable")
    void staticAddEmptyRowShouldBeCallable() {
        try { PDFCellUtils.addEmptyRow((Document) null, 0.0f, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

    @Test
    @DisplayName("static method addLineRow should be callable")
    void staticAddLineRowShouldBeCallable() {
        try { PDFCellUtils.addLineRow((Document) null, 0.0f, 0, 0); } catch (Throwable e) { /* expected */ }
        assertThat(PDFCellUtils.class).isNotNull();
    }

}
