package io.github.easy4j.pdf.template;

import java.io.OutputStream;
import java.util.Map;

import io.github.easy4j.pdf.PdfTemplate;
import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

/**
 * 模板引擎包装基类：引擎把模板渲染为 HTML，再经 HtmlPdfConverter 输出 PDF。
 * 镜像 easydoc 的 AbstractStringTemplateWrappingTemplate 骨架。
 */
public abstract class AbstractStringTemplateWrappingPdfTemplate extends PdfTemplate {

    protected AbstractStringTemplateWrappingPdfTemplate() {
    }

    @Override
    public void process(String template, Map<String, Object> variables, OutputStream out) throws Exception {
        String html = render(template, variables);
        HtmlPdfConverter.htmlToPdf(html, out);
    }

    /** 引擎特有渲染：把 template 内容渲染为 HTML 字符串。 */
    protected abstract String render(String template, Map<String, Object> variables) throws Exception;
}
