package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * PDF → Markdown 门面：内部委托 PdfStructureExtractor 提取结构并序列化。
 * 三入口：File / InputStream / 结构化（返回 DocumentStructure）。
 */
public final class PdfToMarkdownConverter {

    private PdfToMarkdownConverter() {
    }

    public static String pdfToMarkdown(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return PdfStructureExtractor.extract(pdf).toMarkdown();
    }

    public static String pdfToFullMarkdown(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return PdfStructureExtractor.extract(pdf).fullMarkdown();
    }

    public static DocumentStructure pdfToStructured(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return PdfStructureExtractor.extract(pdf);
    }

    public static String pdfToMarkdown(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath());
            return pdfToMarkdown(tmp);
        } finally {
            tmp.delete();
        }
    }

    public static DocumentStructure pdfToStructured(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath());
            return pdfToStructured(tmp);
        } finally {
            tmp.delete();
        }
    }
}
