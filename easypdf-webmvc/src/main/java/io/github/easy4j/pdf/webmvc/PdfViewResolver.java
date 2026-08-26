package io.github.easy4j.pdf.webmvc;

import java.util.Locale;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import io.github.easy4j.pdf.PdfTemplate;

/**
 * 返回 PdfTemplateView 的 ViewResolver；PdfTemplate 通过 setter 注入。
 */
public class PdfViewResolver implements ViewResolver {

    private PdfTemplate template;

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        PdfTemplateView view = new PdfTemplateView();
        view.setTemplate(template);
        view.setTemplateName(viewName);
        return view;
    }

    public void setTemplate(PdfTemplate template) {
        this.template = java.util.Objects.requireNonNull(template, "template must not be null");
    }
}
