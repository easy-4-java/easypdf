package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

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

    // ---------------- Agent API：摘要 / 页区间 / 切片（先看目录树，再按需取内容） ----------------

    /**
     * PDF 文件 → 摘要（页数/字符数/表格数/图片数 + level≤2 章节骨架）。
     * 智能体先看目录树决定要取哪些章节，避免整篇驻留。
     */
    public static DocumentSummary summary(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return DocumentSummaryBuilder.build(pdf, PdfExtractionProperties.defaults());
    }

    /** PDF 输入流 → 摘要（filename 为来源文件名；落临时文件后委托 {@link #summary(File)}）。 */
    public static DocumentSummary summary(InputStream in, String filename) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        Objects.requireNonNull(filename, "filename must not be null");
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return summary(tmp);
        } finally {
            tmp.delete();
        }
    }

    /**
     * PDF 文件 → 页区间 Markdown（fromPage/toPage 均为 1 起算的闭区间）。
     * 按页流式提取并只拼接区间内各页的 partial 结果。
     */
    public static String pageRange(File pdf, int fromPage, int toPage) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        if (fromPage > toPage) {
            throw new IllegalArgumentException("fromPage(" + fromPage + ") > toPage(" + toPage + ")");
        }
        if (fromPage <= 0) {
            throw new IllegalArgumentException("fromPage must be >= 1, was " + fromPage);
        }
        if (toPage <= 0) {
            throw new IllegalArgumentException("toPage must be >= 1, was " + toPage);
        }
        final StringBuilder md = new StringBuilder();
        PdfStructureExtractor.extractPerPage(pdf, PdfExtractionProperties.defaults(),
                new PdfStructureExtractor.PageConsumer() {
                    @Override
                    public boolean page(int pageNo, DocumentStructure partial) {
                        if (partial == null || pageNo < fromPage || pageNo > toPage) {
                            return true;
                        }
                        if (md.length() > 0) {
                            md.append("\n\n");
                        }
                        md.append(partial.toMarkdown());
                        return true;
                    }
                });
        return md.toString();
    }

    /** PDF 文件 → RAG / Embedding 友好的切片流（配置见 {@link ChunkOptions}）。 */
    public static List<DocumentChunk> chunked(File pdf, ChunkOptions opts) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        return DocumentChunker.chunk(PdfStructureExtractor.extract(pdf), opts);
    }
}
