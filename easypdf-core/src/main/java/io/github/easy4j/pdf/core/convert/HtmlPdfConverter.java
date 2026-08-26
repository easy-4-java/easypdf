package io.github.easy4j.pdf.core.convert;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.font.FontProvider;

/**
 * HTML 字符串与 PDF 之间的转换：html2pdf 渲染 + 文本提取。
 * 静态 FontProvider 注册系统字体，保证中文等 CJK 字符可渲染。
 */
public final class HtmlPdfConverter {

    private static final FontProvider FONT_PROVIDER = new FontProvider();

    static {
        FONT_PROVIDER.addSystemFonts();
    }

    private HtmlPdfConverter() {
    }

    /** 追加注册字体文件（服务器缺少系统字体时的扩展点）。 */
    public static void registerFont(String fontPath) {
        Objects.requireNonNull(fontPath, "fontPath must not be null");
        FONT_PROVIDER.addFont(fontPath);
    }

    /** 将 HTML 字符串渲染为 PDF 写入输出流（自动处理中文字体）。 */
    public static void htmlToPdf(String html, OutputStream out) throws IOException {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(out, "out must not be null");
        ConverterProperties props = new ConverterProperties();
        props.setFontProvider(FONT_PROVIDER);
        HtmlConverter.convertToPdf(html, out, props);
    }

    /** 从 PDF 文件逐页提取纯文本。 */
    public static String pdfToText(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        StringBuilder sb = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(pdf))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
        }
        return sb.toString();
    }
}
