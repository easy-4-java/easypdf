package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

/**
 * Markdown ↔ PDF 快速转换门面：mdToHtml → html2pdf → PDF；pdfToText → textToMarkdown。
 */
public final class EasyPdf {

    private EasyPdf() {
    }

    /** Markdown 文本 → PDF 文件（自动渲染标题/表格/代码块/列表等 GFM 语法）。 */
    public static void markdownToPdf(String markdown, File output) throws IOException {
        Objects.requireNonNull(output, "output must not be null");
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            markdownToPdf(markdown, out);
        }
    }

    /** Markdown 文本 → PDF 输出流。 */
    public static void markdownToPdf(String markdown, OutputStream out) throws IOException {
        Objects.requireNonNull(markdown, "markdown must not be null");
        Objects.requireNonNull(out, "out must not be null");
        HtmlPdfConverter.htmlToPdf(MarkdownConverter.mdToHtml(markdown), out);
    }

    /** PDF 文件 → Markdown 文本（尽力而为：文本提取 + 段落整理，结构还原以可读性为准）。 */
    public static String pdfToMarkdown(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return MarkdownConverter.textToMarkdown(HtmlPdfConverter.pdfToText(pdf));
    }

    /** PDF 输入流 → Markdown 文本。 */
    public static String pdfToMarkdown(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return pdfToMarkdown(tmp);
        } finally {
            tmp.delete();
        }
    }

    /** PDF 文件 → 结构化 Markdown（标题/列表/表格，document 顶层；Task 2-3 新增）。 */
    public static String pdfToStructuredMarkdown(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return PdfToMarkdownConverter.pdfToFullMarkdown(pdf);
    }

    /** PDF 文件 → 结构化 Document（智能体按需取章节/表格，Task 2-3 新增）。 */
    public static DocumentStructure pdfToStructured(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return PdfToMarkdownConverter.pdfToStructured(pdf);
    }

    /** PDF 输入流 → 结构化 Markdown。 */
    public static String pdfToStructuredMarkdown(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return pdfToStructuredMarkdown(tmp);
        } finally {
            tmp.delete();
        }
    }

    /** Markdown → Tagged PDF 文件（无损往返用：pdfToStructuredMarkdown 可语义级还原）。 */
    public static void markdownToPdfTagged(String markdown, File output) throws IOException {
        Objects.requireNonNull(output, "output must not be null");
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            markdownToPdfTagged(markdown, out);
        }
    }

    /** Markdown → Tagged PDF 输出流。 */
    public static void markdownToPdfTagged(String markdown, OutputStream out) throws IOException {
        Objects.requireNonNull(markdown, "markdown must not be null");
        Objects.requireNonNull(out, "out must not be null");
        HtmlPdfConverter.htmlToPdfTagged(MarkdownConverter.mdToHtml(markdown), out);
    }
}
