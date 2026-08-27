package io.github.easy4j.pdf.xhtml.convert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

/**
 * 测试辅助：统一 PDF 夹具生成，减少各测试文件重复 HtmlPdfConverter.htmlToPdf + 落盘写法。
 */
public final class MarkdownTestSupport {

    private MarkdownTestSupport() {}

    public static File writePdf(File dir, String filename, String html) throws Exception {
        File f = new File(dir, filename);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        Files.write(f.toPath(), out.toByteArray());
        return f;
    }

    public static File writeTaggedPdf(File dir, String filename, String html) throws Exception {
        File f = new File(dir, filename);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdfTagged(html, out);
        Files.write(f.toPath(), out.toByteArray());
        return f;
    }
}
