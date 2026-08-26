package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

/**
 * Markdown ↔ PDF 快速转换门面：mdToHtml → html2pdf → PDF；pdfToText → textToMarkdown。
 */
public final class EasyPdf {

    private EasyPdf() {
    }

    /** Markdown 文本 → PDF 文件（自动渲染标题/表格/代码块/列表等 GFM 语法）。 */
    public static void markdownToPdf(String markdown, File output) throws IOException {
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            markdownToPdf(markdown, out);
        }
    }

    /** Markdown 文本 → PDF 输出流。 */
    public static void markdownToPdf(String markdown, OutputStream out) throws IOException {
        HtmlPdfConverter.htmlToPdf(MarkdownConverter.mdToHtml(markdown), out);
    }

    /** PDF 文件 → Markdown 文本（尽力而为：文本提取 + 段落整理，结构还原以可读性为准）。 */
    public static String pdfToMarkdown(File pdf) throws IOException {
        return MarkdownConverter.textToMarkdown(HtmlPdfConverter.pdfToText(pdf));
    }

    /** PDF 输入流 → Markdown 文本。 */
    public static String pdfToMarkdown(InputStream in) throws IOException {
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return pdfToMarkdown(tmp);
        } finally {
            tmp.delete();
        }
    }
}
