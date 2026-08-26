package io.github.easy4j.pdf.webmvc;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

import io.github.easy4j.pdf.PdfTemplate;

/**
 * Spring MVC 视图：委托 PdfTemplate 渲染 PDF，先缓冲后写响应。
 */
public class PdfTemplateView extends AbstractView {

    public static final String PDF_CONTENT_TYPE = "application/pdf";

    private PdfTemplate template;
    private String templateName;

    public PdfTemplateView() {
        setContentType(PDF_CONTENT_TYPE);
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        java.io.ByteArrayOutputStream baos = createTemporaryOutputStream();
        template.process(templateName, model, baos);
        writeToResponse(response, baos);
    }

    public void setTemplate(PdfTemplate template) {
        this.template = java.util.Objects.requireNonNull(template, "template must not be null");
    }

    public void setTemplateName(String templateName) {
        this.templateName = java.util.Objects.requireNonNull(templateName, "templateName must not be null");
    }
}
